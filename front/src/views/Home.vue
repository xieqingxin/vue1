<template>
  <div class="home">
    <header class="topbar">
      <div class="brand">
        <span class="brand__dot">见</span>
        <span>用户中心</span>
      </div>
      <div class="user-info">
        <span class="avatar" aria-hidden="true">
          <img v-if="user && user.avatar" :src="user.avatar" alt="avatar" />
          <template v-else>{{ initial }}</template>
        </span>
        <span class="name">{{ user ? user.nickname || user.username : '' }}</span>
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

        <div v-if="listError" class="msg error">{{ listError }}</div>

        <ul v-if="visibleTasks.length" class="task-list">
          <li v-for="t in visibleTasks" :key="t.id" class="task-card" :class="{ done: t.status === 1 }">
            <div class="task-card__main">
              <div class="task-card__head">
                <span class="task-status" :class="t.status === 1 ? 'is-done' : 'is-doing'">
                  {{ t.status === 1 ? '已完成' : '进行中' }}
                </span>
                <h3 class="task-card__name">{{ t.name }}</h3>
                <span class="task-card__reward">奖励 ¥{{ formatMoney(t.reward) }}</span>
              </div>
              <p v-if="t.content" class="task-card__content">{{ t.content }}</p>
              <div class="task-card__meta">
                <span>开始 {{ formatTime(t.startTime) }}</span>
                <span class="dot">·</span>
                <span>截止 {{ formatTime(t.endTime) }}</span>
              </div>
            </div>
            <div class="task-card__actions">
              <button v-if="t.status === 0" class="act act--done" @click="onToggle(t, 1)">标记完成</button>
              <button v-else class="act" @click="onToggle(t, 0)">重新打开</button>
              <button class="act act--danger" @click="onDelete(t)">删除</button>
            </div>
          </li>
        </ul>
        <p v-else class="empty">{{ tasks.length ? '该筛选条件下暂无任务' : '暂无任务，先在上方提交一个吧。' }}</p>
      </section>
    </main>
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
const creating = ref(false)
const createError = ref('')
const listError = ref('')

const form = ref({ name: '', content: '', startTime: '', endTime: '', reward: '' })

const filters = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'doing' },
  { label: '已完成', value: 'done' }
]

const visibleTasks = computed(() => {
  if (filter.value === 'doing') return tasks.value.filter((t) => t.status === 0)
  if (filter.value === 'done') return tasks.value.filter((t) => t.status === 1)
  return tasks.value
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
      startTime: f.startTime.length === 16 ? f.startTime + ':00' : f.startTime,
      endTime: f.endTime.length === 16 ? f.endTime + ':00' : f.endTime,
      reward: Number(f.reward)
    })
    form.value = { name: '', content: '', startTime: '', endTime: '', reward: '' }
    await loadTasks()
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

async function onToggle(t, status) {
  listError.value = ''
  try {
    await updateTaskStatus(t.id, status)
    await loadTasks()
  } catch (e) {
    listError.value = e.message
  }
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

.task-card.done {
  border-left-color: #94a3b8;
  opacity: 0.75;
}

.task-card.done .task-card__name {
  text-decoration: line-through;
  color: var(--ink-faint);
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

.task-card__name {
  font-size: 15.5px;
  font-weight: 650;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
