<template>
  <div class="detail-page">
    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goBack">← 返回任务列表</button>
        <div class="brand">
          <span class="brand__dot">见</span>
          <span>用户中心</span>
        </div>
      </div>
    </header>

    <main class="container">
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="loadError" class="state error">{{ loadError }}</div>
      <div v-else-if="!task" class="state">任务不存在或已被删除。<button class="link-like" @click="goBack">返回列表</button></div>

      <template v-else>
        <!-- 概要区 -->
        <section class="hero" :class="'accent-' + taskType">
          <div class="hero__badges stagger-item">
            <span class="badge badge--type">{{ typeMeta.icon }} {{ typeMeta.label }}</span>
            <span class="badge" :class="'badge--' + statusKey">{{ statusLabel }}</span>
          </div>
          <h1 class="hero__name stagger-item">{{ task.name }}</h1>
          <p v-if="task.content" class="hero__content stagger-item">{{ task.content }}</p>
          <div class="hero__reward stagger-item">
            <span class="hero__reward-num">¥{{ formatMoney(task.reward) }}</span>
            <span class="hero__reward-label">完成奖励</span>
          </div>
        </section>

        <!-- 时间线进度 -->
        <section v-if="task.status === 0" class="timeline">
          <div class="timeline__head">
            <span>任务进度</span>
            <span class="timeline__countdown" :class="{ late: countdown.late }">
              {{ countdown.late ? `已超时 ${formatDuration(countdown.abs)}` : `剩余 ${formatDuration(countdown.abs)}` }}
            </span>
          </div>
          <div class="timeline__track">
            <div class="timeline__fill" :style="{ width: progress + '%' }"></div>
            <span class="timeline__thumb" :style="{ left: progress + '%' }"></span>
          </div>
          <div class="timeline__ends">
            <span>{{ formatTime(task.startTime) }}</span>
            <span>{{ formatTime(task.endTime) }}</span>
          </div>
        </section>

        <!-- 字段明细 -->
        <section class="fields">
          <h2 class="fields__title">任务信息</h2>
          <dl class="fields__list">
            <div class="field">
              <dt>任务类型</dt>
              <dd>{{ typeMeta.icon }} {{ typeMeta.label }}</dd>
            </div>
            <div class="field">
              <dt>当前状态</dt>
              <dd><span class="badge" :class="'badge--' + statusKey">{{ statusLabel }}</span></dd>
            </div>
            <div class="field">
              <dt>开始时间</dt>
              <dd>{{ formatTime(task.startTime) }}</dd>
            </div>
            <div class="field">
              <dt>截止时间</dt>
              <dd>{{ formatTime(task.endTime) }}</dd>
            </div>
            <div v-if="task.status === 1 && task.completedAt" class="field">
              <dt>完成时间</dt>
              <dd class="dd--done">{{ formatTime(task.completedAt) }}</dd>
            </div>
            <div class="field">
              <dt>创建时间</dt>
              <dd>{{ formatTime(task.createdAt) }}</dd>
            </div>
          </dl>
        </section>

        <!-- 操作区 -->
        <section class="actions">
          <button class="btn btn--danger" @click="onDelete">删除任务</button>
          <button v-if="task.status === 0" class="btn btn--primary" :disabled="completing" @click="askComplete">
            {{ completing ? '提交中...' : '任务完成' }}
          </button>
        </section>
      </template>
    </main>

    <!-- 完成确认弹窗 -->
    <div v-if="showConfirm" class="modal-mask" @click.self="closeConfirm">
      <div class="modal" role="dialog" aria-modal="true">
        <h3 class="modal__title">确认完成任务</h3>
        <p class="modal__text">是否确认任务完成？</p>
        <div class="modal__actions">
          <button class="btn btn--ghost" @click="closeConfirm">取消</button>
          <button class="btn btn--primary" :disabled="completing" @click="confirmComplete">
            {{ completing ? '提交中...' : '确认完成' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 完成鼓励弹窗 -->
    <div v-if="praiseText" class="modal-mask" @click.self="closePraise">
      <div class="modal modal--praise" role="dialog" aria-modal="true">
        <div class="praise__icon" aria-hidden="true">🎉</div>
        <h3 class="modal__title">任务完成！</h3>
        <p class="modal__text praise__text">{{ praiseText }}</p>
        <div class="modal__actions">
          <button class="btn btn--primary" @click="closePraise">太棒了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listTasks, updateTaskStatus, deleteTask } from '../api/user'

const route = useRoute()
const router = useRouter()

const task = ref(null)
const loading = ref(true)
const loadError = ref('')
const showConfirm = ref(false)
const completing = ref(false)
const praiseText = ref('')
const now = ref(Date.now())

const TASK_TYPES = {
  exercise: { label: '锻炼', icon: '🏃' },
  work: { label: '工作', icon: '💼' },
  study: { label: '学习', icon: '📚' },
  life: { label: '生活', icon: '🏠' },
  other: { label: '其他', icon: '📌' }
}

const taskType = computed(() => (task.value && task.value.type) || 'other')
const typeMeta = computed(() => TASK_TYPES[taskType.value] || TASK_TYPES.other)

const statusKey = computed(() => {
  if (!task.value) return 'doing'
  if (task.value.status === 1) return 'done'
  if (task.value.status === 2) return 'expired'
  return 'doing'
})

const statusLabel = computed(() => {
  if (statusKey.value === 'done') return '已完成'
  if (statusKey.value === 'expired') return '已过期'
  return '进行中'
})

// 时间线进度：开始 -> 截止
const progress = computed(() => {
  const t = task.value
  if (!t || !t.startTime || !t.endTime) return 0
  const s = new Date(String(t.startTime).replace('T', ' ').replace(/-/g, '/')).getTime()
  const e = new Date(String(t.endTime).replace('T', ' ').replace(/-/g, '/')).getTime()
  if (!(e > s)) return 0
  const p = ((now.value - s) / (e - s)) * 100
  return Math.min(100, Math.max(0, p))
})

// 倒计时：相对截止时间，负值即超时
const countdown = computed(() => {
  const t = task.value
  if (!t || !t.endTime) return { abs: 0, late: false }
  const e = new Date(String(t.endTime).replace('T', ' ').replace(/-/g, '/')).getTime()
  const diff = e - now.value
  return { abs: Math.abs(diff), late: diff < 0 }
})

let timer = null

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function formatMoney(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function formatDuration(ms) {
  const totalMin = Math.floor(ms / 60000)
  const days = Math.floor(totalMin / 1440)
  const hours = Math.floor((totalMin % 1440) / 60)
  const mins = totalMin % 60
  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${mins}分`
  return `${mins}分钟`
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listTasks()
    const id = Number(route.params.id)
    task.value = (res.data || []).find((t) => t.id === id) || null
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/home')
}

const PRAISES = [
  '干得漂亮！又搞定一件事，继续保持这股劲头 💪',
  '太棒了！你的执行力真不是盖的 ✨',
  '完美收官！这一步走得很稳，给自己鼓个掌 👏',
  '又完成一个！离目标更近了一步，加油 🚀',
  '厉害！专注的人最有魅力，继续保持 🔥',
  '漂亮！小步快跑，积累就是力量 🌱',
  '你做到了！每一份努力都不会白费 🏆',
  '收工！适度休息，下一件事会更有精神 ☕'
]

function askComplete() {
  showConfirm.value = true
}

function closeConfirm() {
  if (completing.value) return
  showConfirm.value = false
}

async function confirmComplete() {
  if (!task.value || completing.value) return
  completing.value = true
  try {
    await updateTaskStatus(task.value.id, 1)
    showConfirm.value = false
    await load()
    praiseText.value = PRAISES[Math.floor(Math.random() * PRAISES.length)]
  } catch (e) {
    loadError.value = e.message
  } finally {
    completing.value = false
  }
}

function closePraise() {
  praiseText.value = ''
}

async function onDelete() {
  if (!task.value) return
  if (!window.confirm(`确定删除任务「${task.value.name}」吗？`)) return
  try {
    await deleteTask(task.value.id)
    router.push('/home')
  } catch (e) {
    loadError.value = e.message
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  min-height: 100svh;
  background:
    radial-gradient(720px 320px at 85% -10%, rgba(22, 160, 133, 0.1), transparent 65%),
    radial-gradient(560px 280px at -5% 0%, rgba(125, 211, 252, 0.14), transparent 60%),
    var(--canvas);
}

/* ---------- 顶栏 ---------- */
.topbar {
  height: 62px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(16px, 4vw, 32px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar__left {
  display: inline-flex;
  align-items: center;
  gap: 16px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 650;
  color: var(--ink);
}

.brand__dot {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: var(--accent);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
}

.back-btn {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-soft);
  cursor: pointer;
  font-size: 13px;
  padding: 7px 14px;
  border-radius: 999px;
  transition: color 0.18s, border-color 0.18s, transform 0.18s;
}

.back-btn:hover {
  color: var(--accent-deep);
  border-color: var(--accent);
  transform: translateX(-2px);
}

/* ---------- 布局 ---------- */
.container {
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 44px) clamp(16px, 4vw, 24px) 72px;
}

.state {
  text-align: center;
  padding: 80px 0;
  color: var(--ink-faint);
  font-size: 14.5px;
}

.state.error {
  color: var(--danger);
}

.link-like {
  border: none;
  background: none;
  color: var(--accent-deep);
  font-size: 14.5px;
  cursor: pointer;
  text-decoration: underline;
}

/* ---------- 概要区 ---------- */
.hero {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: clamp(24px, 4vw, 36px);
  overflow: hidden;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.05s both;
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: var(--type-color, var(--accent));
}

.hero::after {
  content: '';
  position: absolute;
  top: -60px;
  right: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: var(--type-soft, var(--accent-soft));
  opacity: 0.55;
  pointer-events: none;
}

.hero.accent-exercise { --type-color: #e2703a; --type-soft: rgba(226, 112, 58, 0.12); }
.hero.accent-work    { --type-color: #2563eb; --type-soft: rgba(37, 99, 235, 0.1); }
.hero.accent-study   { --type-color: #7c3aed; --type-soft: rgba(124, 58, 237, 0.1); }
.hero.accent-life    { --type-color: #0d9488; --type-soft: rgba(13, 148, 136, 0.1); }
.hero.accent-other   { --type-color: #64748b; --type-soft: rgba(100, 116, 139, 0.1); }

.hero__badges {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  position: relative;
}

/* 概要区内容依次浮现 */
.hero .stagger-item {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero .stagger-item:nth-child(1) { animation-delay: 0.12s; }
.hero .stagger-item:nth-child(2) { animation-delay: 0.18s; }
.hero .stagger-item:nth-child(3) { animation-delay: 0.24s; }
.hero .stagger-item:nth-child(4) { animation-delay: 0.3s; }

.badge {
  font-size: 12px;
  font-weight: 650;
  padding: 4px 11px;
  border-radius: 999px;
}

.badge--type {
  color: #fff;
  background: var(--type-color, var(--accent));
}

.badge--doing {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.badge--done {
  background: #eef2f6;
  color: #64748b;
}

.badge--expired {
  background: #fee2e2;
  color: #dc2626;
}

.hero__name {
  font-size: clamp(22px, 3.4vw, 30px);
  font-weight: 650;
  letter-spacing: -0.015em;
  line-height: 1.3;
  margin-bottom: 10px;
  position: relative;
}

.hero__content {
  font-size: 15px;
  color: var(--ink-soft);
  line-height: 1.7;
  max-width: 56ch;
  position: relative;
}

.hero__reward {
  margin-top: 20px;
  display: flex;
  align-items: baseline;
  gap: 10px;
  position: relative;
}

.hero__reward-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--type-color, var(--accent-deep));
  letter-spacing: -0.01em;
}

.hero__reward-label {
  font-size: 13px;
  color: var(--ink-faint);
}

/* ---------- 时间线 ---------- */
.timeline {
  margin-top: 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px clamp(20px, 3vw, 28px);
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.14s both;
}

.timeline__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.04em;
  color: var(--ink-faint);
  margin-bottom: 14px;
}

.timeline__countdown {
  font-weight: 600;
  color: var(--accent-deep);
}

.timeline__countdown.late {
  color: #dc2626;
}

.timeline__track {
  position: relative;
  height: 8px;
  border-radius: 999px;
  background: #eef2f6;
  overflow: visible;
}

.timeline__fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), var(--accent-deep));
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.timeline__thumb {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--surface);
  border: 3px solid var(--accent);
  transform: translate(-50%, -50%);
  box-shadow: 0 2px 6px rgba(23, 50, 44, 0.2);
  transition: left 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.timeline__ends {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--ink-faint);
}

/* ---------- 字段明细 ---------- */
.fields {
  margin-top: 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px clamp(20px, 3vw, 28px);
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.2s both;
}

.fields__title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
  margin-bottom: 6px;
}

.fields__list {
  margin: 0;
}

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 0;
  border-bottom: 1px solid var(--line);
}

.field:last-child {
  border-bottom: none;
}

.field dt {
  font-size: 13.5px;
  color: var(--ink-faint);
  flex-shrink: 0;
}

.field dd {
  font-size: 14px;
  font-weight: 550;
  color: var(--ink);
  text-align: right;
}

.dd--done {
  color: var(--accent-deep);
}

/* ---------- 操作区 ---------- */
.actions {
  margin-top: 26px;
  display: flex;
  gap: 12px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.26s both;
}

.btn {
  flex: 1;
  height: 46px;
  border: none;
  border-radius: 12px;
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s, color 0.18s, transform 0.18s, box-shadow 0.18s;
}

.btn--primary {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 8px 18px -10px rgba(22, 160, 133, 0.7);
}

.btn--primary:hover:not(:disabled) {
  background: var(--accent-deep);
  transform: translateY(-1px);
}

.btn--primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.btn--danger {
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--ink-soft);
}

.btn--danger:hover {
  border-color: #ffd9c2;
  color: var(--danger);
  background: var(--danger-soft);
}

.btn--ghost {
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--ink-soft);
  flex: 0 0 auto;
  padding: 0 22px;
}

.btn--ghost:hover {
  border-color: var(--accent);
  color: var(--accent-deep);
}

/* ---------- 弹窗 ---------- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(23, 50, 44, 0.45);
  backdrop-filter: blur(3px);
  display: grid;
  place-items: center;
  padding: 20px;
  animation: fadeIn 0.2s ease both;
}

.modal {
  width: min(400px, 100%);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: 24px;
  animation: pop 0.28s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.modal__title {
  font-size: 17px;
  font-weight: 650;
  color: var(--ink);
  margin-bottom: 10px;
}

.modal__text {
  font-size: 14px;
  color: var(--ink-soft);
  line-height: 1.7;
}

.modal__text strong {
  color: var(--ink);
}

.modal__actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal__actions .btn {
  flex: 1;
}

.modal--praise {
  text-align: center;
}

.praise__icon {
  font-size: 44px;
  line-height: 1;
  margin-bottom: 12px;
  animation: bounce 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.15s both;
}

.praise__text {
  font-size: 15px;
}

.modal--praise .modal__actions {
  justify-content: center;
}

.modal--praise .btn {
  flex: 0 0 auto;
  padding: 0 30px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes pop {
  from { opacity: 0; transform: translateY(14px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes bounce {
  0% { transform: scale(0.4); }
  60% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

@media (max-width: 560px) {
  .hero__reward-num {
    font-size: 22px;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
