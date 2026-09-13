<template>
  <div class="home">
    <header class="topbar">
      <div class="brand">
        <span class="brand__dot">见</span>
        <span>用户中心</span>
      </div>
      <div class="user-info">
        <span class="avatar" aria-hidden="true" @click="router.push('/profile')">
          <img v-if="user && user.avatar" :src="user.avatar" alt="avatar" />
          <template v-else>{{ initial }}</template>
        </span>
        <span class="name">{{ user ? user.nickname || user.username : '' }}</span>
        <button class="link-btn" @click="router.push('/profile')">个人中心</button>
        <button class="link-btn" @click="onLogout">退出登录</button>
      </div>
    </header>

    <main class="container">
      <div class="greeting stagger">
        <h1>{{ greeting }}，{{ user ? user.nickname || user.username : '' }}</h1>
        <p>在这里创建任务、跟踪进度，每个任务都像一个订单。</p>
      </div>

      <!-- 添加任务 -->
      <section class="panel create-panel">
        <h2 class="section-title">添加任务</h2>
        <div v-if="createError" class="msg error">{{ createError }}</div>

        <form class="create-form" @submit.prevent="onCreate">
          <div class="form-item">
            <label for="t-name">任务名</label>
            <input id="t-name" v-model.trim="form.name" type="text" placeholder="例如：整理季度报表" maxlength="100" />
          </div>
          <div class="form-item">
            <label for="t-content">任务内容</label>
            <input id="t-content" v-model.trim="form.content" type="text" placeholder="简要描述要做的事（选填）" maxlength="1000" />
          </div>
          <div class="form-item">
            <label>任务类型</label>
            <div class="type-picker">
              <button
                v-for="tp in taskTypes"
                :key="tp.value"
                type="button"
                class="type-chip"
                :class="['type-chip--' + tp.value, { active: form.type === tp.value }]"
                @click="form.type = tp.value"
              >
                {{ tp.icon }} {{ tp.label }}
              </button>
            </div>
          </div>
          <div class="form-grid">
            <div class="form-item">
              <label for="t-start">开始时间</label>
              <input id="t-start" v-model="form.startTime" type="datetime-local" />
            </div>
            <div class="form-item">
              <label for="t-end">结束时间</label>
              <input id="t-end" v-model="form.endTime" type="datetime-local" />
            </div>
          </div>
          <div class="form-grid">
            <div class="form-item">
              <label for="t-reward">完成奖励</label>
              <div class="reward-input">
                <span class="reward-prefix">¥</span>
                <input id="t-reward" v-model="form.reward" type="number" min="0" step="0.01" placeholder="0.00" />
              </div>
            </div>
            <div class="form-item form-item--action">
              <button class="btn-create" type="submit" :disabled="creating">
                {{ creating ? '提交中...' : '提交任务' }}
              </button>
            </div>
          </div>
        </form>
      </section>

      <!-- 任务列表 -->
      <section class="task-section">
        <div class="task-section__head">
          <h2 class="section-title">全部任务</h2>
          <div class="filters">
            <button
              v-for="f in filters"
              :key="f.value"
              class="filter-btn"
              :class="{ active: filter === f.value }"
              @click="filter = f.value"
            >
              {{ f.label }}
            </button>
          </div>
        </div>

        <!-- 任务类型筛选 -->
        <div class="type-filters">
          <button
            class="type-filter-btn"
            :class="{ active: typeFilter === 'all' }"
            @click="typeFilter = 'all'"
          >
            📋 全部类型
          </button>
          <button
            v-for="tp in taskTypes"
            :key="tp.value"
            class="type-filter-btn"
            :class="['type-filter-btn--' + tp.value, { active: typeFilter === tp.value }]"
            @click="typeFilter = tp.value"
          >
            {{ tp.icon }} {{ tp.label }}
          </button>
        </div>

        <div v-if="listError" class="msg error">{{ listError }}</div>

        <ul v-if="visibleTasks.length" class="task-list">
          <li
            v-for="t in visibleTasks"
            :key="t.id"
            class="task-card"
            :class="['type-' + (t.type || 'other'), { done: t.status === 1, expired: t.status === 2 }]"
          >
            <div class="task-card__main">
              <div class="task-card__head">
                <span class="task-status" :class="statusClass(t.status)">{{ statusLabel(t.status) }}</span>
                <span class="task-type-tag" :class="'tag-' + (t.type || 'other')">
                  {{ typeLabel(t.type) }}
                </span>
                <h3 class="task-card__name">
                  <router-link :to="`/task/${t.id}`" class="task-card__link">{{ t.name }}</router-link>
                </h3>
                <span class="task-card__reward">奖励 ¥{{ formatMoney(t.reward) }}</span>
              </div>
              <p v-if="t.content" class="task-card__content">{{ t.content }}</p>
              <div class="task-card__meta">
                <span>开始 {{ formatTime(t.startTime) }}</span>
                <span class="dot">·</span>
                <span>截止 {{ formatTime(t.endTime) }}</span>
                <template v-if="t.status === 1 && t.completedAt">
                  <span class="dot">·</span>
                  <span class="meta-done">完成于 {{ formatTime(t.completedAt) }}</span>
                </template>
              </div>
            </div>
            <div class="task-card__actions">
              <button class="act" @click="router.push(`/task/${t.id}`)">详情</button>
              <button v-if="t.status === 0" class="act act--done" @click="askComplete(t)">标记完成</button>
              <button class="act act--danger" @click="onDelete(t)">删除</button>
            </div>
          </li>
        </ul>
        <p v-else class="empty">{{ tasks.length ? '该筛选条件下暂无任务' : '暂无任务，先在上方提交一个吧。' }}</p>
      </section>
    </main>

    <!-- 完成确认弹窗 -->
    <div v-if="confirmTask" class="modal-mask" @click.self="closeConfirm">
      <div class="modal" role="dialog" aria-modal="true">
        <h3 class="modal__title">确认完成任务</h3>
        <p class="modal__text">
          任务「<strong>{{ confirmTask.name }}</strong>」确认任务是否成？。
        </p>
        <div class="modal__actions">
          <button class="act" @click="closeConfirm">取消</button>
          <button class="act act--done" :disabled="completing" @click="confirmComplete">
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
          <button class="act act--done" @click="closePraise">太棒了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, createTask, listTasks, updateTaskStatus, deleteTask } from '../api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()

const user = ref(store.user)
const tasks = ref([])
const filter = ref('all')
const typeFilter = ref('all')
const creating = ref(false)
const createError = ref('')
const listError = ref('')

const confirmTask = ref(null)
const completing = ref(false)
const praiseText = ref('')

const form = ref({ name: '', content: '', type: 'work', startTime: '', endTime: '', reward: '' })

// 任务类型定义
const taskTypes = [
  { label: '锻炼', value: 'exercise', icon: '🏃' },
  { label: '工作', value: 'work', icon: '💼' },
  { label: '学习', value: 'study', icon: '📚' },
  { label: '生活', value: 'life', icon: '🏠' },
  { label: '其他', value: 'other', icon: '📌' }
]

function typeLabel(v) {
  const tp = taskTypes.find((t) => t.value === (v || 'other'))
  return tp ? `${tp.icon} ${tp.label}` : '📌 其他'
}

function statusLabel(s) {
  if (s === 1) return '已完成'
  if (s === 2) return '已过期'
  return '进行中'
}

function statusClass(s) {
  if (s === 1) return 'is-done'
  if (s === 2) return 'is-expired'
  return 'is-doing'
}

const filters = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'doing' },
  { label: '已完成', value: 'done' },
  { label: '已过期', value: 'expired' }
]

const visibleTasks = computed(() => {
  let list = tasks.value
  if (typeFilter.value !== 'all') {
    list = list.filter((t) => (t.type || 'other') === typeFilter.value)
  }
  if (filter.value === 'doing') return list.filter((t) => t.status === 0)
  if (filter.value === 'done') return list.filter((t) => t.status === 1)
  if (filter.value === 'expired') return list.filter((t) => t.status === 2)
  return list
})

const initial = computed(() => {
  const name = user.value ? user.value.nickname || user.value.username : ''
  return name ? name.charAt(0).toUpperCase() : '?'
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function formatMoney(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

async function loadProfile() {
  try {
    const res = await getProfile()
    user.value = res.data
    store.setUser(res.data)
  } catch (e) {
    listError.value = e.message
  }
}

async function loadTasks() {
  listError.value = ''
  try {
    const res = await listTasks()
    tasks.value = res.data || []
  } catch (e) {
    listError.value = e.message
  }
}

async function onCreate() {
  createError.value = ''
  const f = form.value
  if (!f.name) {
    createError.value = '请填写任务名'
    return
  }
  if (!f.startTime || !f.endTime) {
    createError.value = '请选择开始时间和结束时间'
    return
  }
  if (new Date(f.endTime) <= new Date(f.startTime)) {
    createError.value = '结束时间必须晚于开始时间'
    return
  }
  if (f.reward === '' || Number(f.reward) < 0 || !Number.isFinite(Number(f.reward))) {
    createError.value = '请填写正确的完成奖励'
    return
  }
  creating.value = true
  try {
    await createTask({
      name: f.name,
      content: f.content,
      type: f.type,
      startTime: f.startTime.length === 16 ? f.startTime + ':00' : f.startTime,
      endTime: f.endTime.length === 16 ? f.endTime + ':00' : f.endTime,
      reward: Number(f.reward)
    })
    form.value = { name: '', content: '', type: f.type, startTime: '', endTime: '', reward: '' }
    await loadTasks()
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

// 随机完成鼓励话语
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

function askComplete(t) {
  confirmTask.value = t
}

function closeConfirm() {
  if (completing.value) return
  confirmTask.value = null
}

async function confirmComplete() {
  const t = confirmTask.value
  if (!t || completing.value) return
  completing.value = true
  listError.value = ''
  try {
    await updateTaskStatus(t.id, 1)
    confirmTask.value = null
    await loadTasks()
    praiseText.value = PRAISES[Math.floor(Math.random() * PRAISES.length)]
  } catch (e) {
    listError.value = e.message
  } finally {
    completing.value = false
  }
}

function closePraise() {
  praiseText.value = ''
}

async function onDelete(t) {
  if (!window.confirm(`确定删除任务「${t.name}」吗？`)) return
  listError.value = ''
  try {
    await deleteTask(t.id)
    await loadTasks()
  } catch (e) {
    listError.value = e.message
  }
}

function onLogout() {
  store.logout()
  router.push('/login')
}

onMounted(() => {
  loadProfile()
  loadTasks()
})
</script>

<style scoped>
.home {
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

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent-soft);
  color: var(--accent-deep);
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  font-size: 14px;
  color: var(--ink);
}

.link-btn {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-soft);
  cursor: pointer;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 999px;
  transition: color 0.18s, border-color 0.18s;
}

.link-btn:hover {
  color: var(--danger);
  border-color: #ffd9c2;
}

/* ---------- 布局 ---------- */
.container {
  max-width: 860px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 44px) clamp(16px, 4vw, 24px) 64px;
}

.greeting {
  margin-bottom: 24px;
}

.greeting h1 {
  font-size: clamp(24px, 3.2vw, 32px);
  font-weight: 650;
  letter-spacing: -0.015em;
  margin-bottom: 8px;
}

.greeting p {
  font-size: 14.5px;
  color: var(--ink-soft);
}

.section-title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
}

.panel {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: clamp(18px, 2.6vw, 26px);
}

/* ---------- 添加任务 ---------- */
.create-panel {
  margin-bottom: 28px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.create-panel .section-title {
  margin-bottom: 16px;
}

.create-form .form-item {
  margin-bottom: 14px;
}

.create-form label {
  display: block;
  font-size: 13px;
  font-weight: 550;
  margin-bottom: 6px;
  color: var(--ink);
}

.create-form input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 11px;
  font-size: 14px;
  font-family: inherit;
  color: var(--ink);
  background: var(--surface);
  outline: none;
  transition: border-color 0.18s, box-shadow 0.18s;
}

.create-form input:hover {
  border-color: #c9ddd6;
}

.create-form input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(22, 160, 133, 0.12);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.reward-input {
  position: relative;
}

.reward-prefix {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  color: var(--ink-faint);
  pointer-events: none;
}

.reward-input input {
  padding-left: 28px;
}

.form-item--action {
  display: flex;
  align-items: flex-end;
}

.btn-create {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 11px;
  background: var(--accent);
  color: #fff;
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 18px -10px rgba(22, 160, 133, 0.7);
  transition: background 0.18s, transform 0.18s;
}

.btn-create:hover:not(:disabled) {
  background: var(--accent-deep);
  transform: translateY(-1px);
}

.btn-create:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* ---------- 任务列表 ---------- */
.task-section {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.16s both;
}

.task-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}

.filters {
  display: inline-flex;
  gap: 6px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 4px;
}

.filter-btn {
  border: none;
  background: none;
  font-size: 13px;
  color: var(--ink-soft);
  padding: 5px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}

.filter-btn.active {
  background: var(--accent);
  color: #fff;
  font-weight: 600;
}

/* ---------- 任务类型筛选按钮 ---------- */
.type-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.type-filter-btn {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.type-filter-btn:hover {
  border-color: #c9ddd6;
  color: var(--ink);
}

.type-filter-btn.active {
  font-weight: 600;
  color: #fff;
  border-color: transparent;
  background: #64748b;
}

.type-filter-btn--exercise.active {
  background: #e2703a;
}

.type-filter-btn--work.active {
  background: #2563eb;
}

.type-filter-btn--study.active {
  background: #7c3aed;
}

.type-filter-btn--life.active {
  background: #0d9488;
}

.type-filter-btn--other.active {
  background: #64748b;
}

/* ---------- 表单中的类型选择 ---------- */
.type-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-chip {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 13px;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.type-chip:hover {
  border-color: #c9ddd6;
}

.type-chip.active {
  font-weight: 600;
  color: #fff;
  border-color: transparent;
  background: #64748b;
}

.type-chip--exercise.active {
  background: #e2703a;
}

.type-chip--work.active {
  background: #2563eb;
}

.type-chip--study.active {
  background: #7c3aed;
}

.type-chip--life.active {
  background: #0d9488;
}

.type-chip--other.active {
  background: #64748b;
}

.task-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-left: 4px solid var(--accent);
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.18s, transform 0.18s, border-left-color 0.18s;
}

.task-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.task-card.done .task-card__name {
  text-decoration: line-through;
  color: var(--ink-faint);
}

/* 不同任务类型的卡片样式 */
.task-card.type-exercise {
  border-left-color: #e2703a;
  background: linear-gradient(90deg, rgba(226, 112, 58, 0.06), var(--surface) 42%);
}

.task-card.type-work {
  border-left-color: #2563eb;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.06), var(--surface) 42%);
}

.task-card.type-study {
  border-left-color: #7c3aed;
  background: linear-gradient(90deg, rgba(124, 58, 237, 0.06), var(--surface) 42%);
}

.task-card.type-life {
  border-left-color: #0d9488;
  background: linear-gradient(90deg, rgba(13, 148, 136, 0.06), var(--surface) 42%);
}

.task-card.type-other {
  border-left-color: #64748b;
  background: linear-gradient(90deg, rgba(100, 116, 139, 0.05), var(--surface) 42%);
}

.task-card.done {
  border-left-color: #94a3b8 !important;
  opacity: 0.75;
  background: var(--surface);
}

.task-card.expired {
  border-left-color: #dc2626;
  background: linear-gradient(90deg, rgba(220, 38, 38, 0.05), var(--surface) 42%);
}

.task-card.expired .task-card__name {
  color: var(--ink-faint);
}

/* 任务类型标签 */
.task-type-tag {
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 6px;
  flex-shrink: 0;
}

.task-type-tag.tag-exercise {
  background: rgba(226, 112, 58, 0.12);
  color: #c2551d;
}

.task-type-tag.tag-work {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.task-type-tag.tag-study {
  background: rgba(124, 58, 237, 0.1);
  color: #6d28d9;
}

.task-type-tag.tag-life {
  background: rgba(13, 148, 136, 0.1);
  color: #0f766e;
}

.task-type-tag.tag-other {
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
}

.task-card__main {
  min-width: 0;
}

.task-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.task-status {
  font-size: 11.5px;
  font-weight: 650;
  padding: 3px 9px;
  border-radius: 999px;
  flex-shrink: 0;
}

.task-status.is-doing {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.task-status.is-done {
  background: #eef2f6;
  color: #64748b;
}

.task-status.is-expired {
  background: #fee2e2;
  color: #dc2626;
}

.task-card__name {
  font-size: 15.5px;
  font-weight: 650;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__link {
  color: inherit;
  text-decoration: none;
  transition: color 0.18s;
}

.task-card__link:hover {
  color: var(--accent-deep);
  text-decoration: underline;
}

.task-card__reward {
  margin-left: auto;
  font-size: 13.5px;
  font-weight: 650;
  color: var(--accent-deep);
  background: var(--accent-soft);
  padding: 3px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}

.task-card__content {
  margin-top: 6px;
  font-size: 13.5px;
  color: var(--ink-soft);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.task-card__meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-faint);
}

.task-card__meta .dot {
  color: #cbd5d1;
}

.task-card__meta .meta-done {
  color: var(--accent-deep);
  font-weight: 600;
}

.task-card__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.act {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 12.5px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
  white-space: nowrap;
}

.act:hover {
  border-color: var(--accent);
  color: var(--accent-deep);
}

.act--done {
  border-color: #bfe6dc;
  color: var(--accent-deep);
  background: var(--accent-soft);
}

.act--done:hover {
  background: var(--accent);
  color: #fff;
}

.act--danger:hover {
  border-color: #ffd9c2;
  color: var(--danger);
  background: var(--danger-soft);
}

.empty {
  color: var(--ink-faint);
  font-size: 14px;
  text-align: center;
  padding: 36px 0;
  background: var(--surface);
  border: 1px dashed var(--line);
  border-radius: 14px;
}

.msg {
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 13.5px;
  margin-bottom: 14px;
  animation: rise 0.28s ease both;
}

.msg.error {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid #ffd9c2;
}

.stagger > * {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
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

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .task-card {
    flex-direction: column;
    align-items: stretch;
  }

  .task-card__actions {
    flex-direction: row;
    justify-content: flex-end;
  }

  .task-card__reward {
    margin-left: 0;
  }
}
</style>
