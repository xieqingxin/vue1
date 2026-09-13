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

      <!-- 任务类型入口：点击标签进入对应的添加页面 -->
      <div class="type-entries">
        <button
          v-for="(tp, i) in taskTypes"
          :key="tp.value"
          class="entry"
          :class="['entry--' + tp.value, { 'entry--paper': tp.value === 'study' }]"
          :style="{ '--rot': i % 2 === 0 ? '-1.2deg' : '1deg' }"
          @click="router.push(`/add/${tp.value}`)"
        >
          <span class="entry__icon">{{ tp.icon }}</span>
          <span class="entry__label">{{ tp.label }}</span>
          <span class="entry__plus">＋添加</span>
        </button>
      </div>

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

        <TaskList
          :tasks="visibleTasks"
          :empty-text="tasks.length ? '该筛选条件下暂无任务' : '暂无任务，点击上方标签添加一个吧。'"
          @refresh="loadTasks"
          @error="(m) => (listError = m)"
        />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, listTasks } from '../api/user'
import { useUserStore } from '../store/user'
import TaskList from '../components/TaskList.vue'

const router = useRouter()
const store = useUserStore()

const user = ref(store.user)
const tasks = ref([])
const filter = ref('all')
const listError = ref('')

// 任务类型定义
const taskTypes = [
  { label: '锻炼', value: 'exercise', icon: '🏃' },
  { label: '工作', value: 'work', icon: '💼' },
  { label: '学习', value: 'study', icon: '📚' },
  { label: '生活', value: 'life', icon: '🏠' },
  { label: '其他', value: 'other', icon: '📌' }
]

const filters = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'doing' },
  { label: '已完成', value: 'done' },
  { label: '已过期', value: 'expired' }
]

const visibleTasks = computed(() => {
  const list = tasks.value
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

/* ---------- 类型入口按钮（贴纸风格） ---------- */
.type-entries {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 30px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.entry {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 108px;
  padding: 16px 10px 13px;
  border-radius: 16px;
  cursor: pointer;
  font-family: inherit;
  transform: rotate(var(--rot, 0deg));
  transition: transform 0.18s, box-shadow 0.18s, filter 0.18s;
}

.entry:hover {
  transform: rotate(0deg) translateY(-3px) scale(1.03);
}

.entry:active {
  transform: rotate(0deg) translateY(0) scale(0.98);
}

.entry__icon {
  font-size: 26px;
  line-height: 1;
}

.entry__label {
  font-size: 15px;
  font-weight: 700;
}

.entry__plus {
  font-size: 11px;
  font-weight: 600;
  opacity: 0.75;
  letter-spacing: 0.05em;
}

/* 普通贴纸：白底 + 彩色描边和硬阴影 */
.entry--exercise,
.entry--work,
.entry--life,
.entry--other {
  background: var(--surface);
  border: 2px solid var(--ec);
  color: var(--ec);
  box-shadow: 3px 4px 0 rgba(23, 50, 44, 0.12), 0 10px 20px -12px rgba(23, 50, 44, 0.3);
}

.entry--exercise { --ec: #e2703a; }
.entry--work { --ec: #2563eb; }
.entry--life { --ec: #0d9488; }
.entry--other { --ec: #64748b; }

.entry--exercise:hover,
.entry--work:hover,
.entry--life:hover,
.entry--other:hover {
  box-shadow: 3px 5px 0 rgba(23, 50, 44, 0.16), 0 14px 24px -12px rgba(23, 50, 44, 0.35);
}

/* 学习贴纸：纸张背景（横线 + 装订线） */
.entry--paper {
  border: 1px solid #e6dfc8;
  color: #6d28d9;
  border-radius: 6px;
  background:
    linear-gradient(90deg, transparent 0 12px, rgba(224, 138, 138, 0.5) 12px 13px, transparent 13px),
    repeating-linear-gradient(transparent 0 15px, rgba(124, 58, 237, 0.14) 15px 16px),
    #fffdf5;
  box-shadow: 3px 4px 0 rgba(23, 50, 44, 0.12), 0 10px 20px -12px rgba(23, 50, 44, 0.3);
}

/* 纸张顶部的胶带 */
.entry--paper::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%) rotate(-3deg);
  width: 52px;
  height: 16px;
  background: rgba(255, 236, 153, 0.75);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
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
  .entry {
    width: calc(33.33% - 10px);
  }
}
</style>
