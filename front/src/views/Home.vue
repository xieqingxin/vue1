<template>
  <div class="home">
    <!-- 场景背景：与添加页同款（实景照片 + 提亮蒙层 + Ken Burns） -->
    <div class="scene" aria-hidden="true">
      <img class="scene__img" :src="sceneImg" alt="" />
      <span class="scene__scrim"></span>
    </div>

    <header class="topbar">
      <div class="brand">
        <span class="brand__dot" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
        </span>
        <span>自律计划任务平台</span>
      </div>
      <div class="user-info">
        <button class="link-btn" @click="router.push('/profile')">个人中心</button>
        <button class="link-btn" @click="onLogout">退出登录</button>
      </div>
    </header>

    <main class="container">
      <div class="greeting stagger">
        <h1>{{ greeting }}，{{ user ? user.nickname || user.username : '' }}</h1>
        <p>在这里创建任务、跟踪进度，自律完成任务。</p>
      </div>

      <!-- 任务类型入口：点击标签进入对应的添加页面（便利贴样式） -->
      <div class="type-entries">
        <button
          v-for="(tp, i) in taskTypes"
          :key="tp.value"
          class="entry"
          :class="'entry--' + tp.value"
          :style="{ '--rot': i % 2 === 0 ? '-1.6deg' : '1.4deg' }"
          @click="router.push(`/add/${tp.value}`)"
        >
          <span class="entry__icon">{{ tp.icon }}</span>
          <span class="entry__label">{{ tp.label }}任务</span>
          <span class="entry__plus">＋添加</span>
        </button>
      </div>

      <!-- 任务列表：便利贴墙（样式见 src/styles/note-wall.css） -->
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

        <div class="note-wall">
          <TaskList
            :tasks="visibleTasks"
            :empty-text="tasks.length ? '该筛选条件下暂无任务' : '暂无任务，点击上方标签添加一个吧。'"
            @refresh="loadTasks"
            @error="(m) => (listError = m)"
          />
        </div>
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

// 主页场景背景：桌面俯拍的实景照片（与添加页同一风格）
const sceneImg = 'https://images.unsplash.com/photo-1497215728101-856f4ea42174?w=1920&q=80&auto=format&fit=crop'

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
  background: var(--canvas);
}

/* ---------- 场景背景 ---------- */
.scene {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: linear-gradient(150deg, #eef1f4 0%, #dde3ea 55%, #c7d0da 100%);
}

.scene__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  animation: kenburns 26s ease-in-out infinite alternate;
}

/* 轻微提亮蒙层，保证纸面内容可读 */
.scene__scrim {
  position: absolute;
  inset: 0;
  background: radial-gradient(90% 80% at 50% 40%, rgba(250, 251, 252, 0.5) 0%, rgba(70, 80, 95, 0.3) 100%);
}

@keyframes kenburns {
  from { transform: scale(1.04); }
  to { transform: scale(1.12) translate3d(-1.5%, -1.5%, 0); }
}

/* ---------- 顶栏 ---------- */
.topbar {
  height: 62px;
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(14px);
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
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
  position: relative;
  z-index: 1;
  max-width: 860px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 44px) clamp(16px, 4vw, 24px) 64px;
}

.greeting {
  margin-bottom: 24px;
  padding: 18px 22px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 6px 18px -10px rgba(30, 41, 59, 0.25);
}

.greeting h1 {
  font-size: clamp(24px, 3.2vw, 32px);
  font-weight: 650;
  letter-spacing: -0.015em;
  margin-bottom: 8px;
  color: var(--ink);
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
  color: var(--ink-soft);
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
}

.panel {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: clamp(18px, 2.6vw, 26px);
}

/* ---------- 类型入口按钮（便利贴风格，颜色与任务列表便签一致） ---------- */
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
  padding: 20px 10px 14px;
  border-radius: 2px 2px 2px 12px;
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

/* 入口便利贴：类型主题浅色纸面 + 纹理 + 深色墨水字 */
.entry--exercise { background: var(--grain), linear-gradient(160deg, #ffe9db 0%, #ffdbc6 70%, #fbcfad 100%); color: #c2551d; }
.entry--work { background: var(--grain), linear-gradient(160deg, #e0ebff 0%, #d2e2fd 70%, #c2d7f9 100%); color: #1d4ed8; }
.entry--study { background: var(--grain), linear-gradient(160deg, #ece2ff 0%, #e0d2fd 70%, #d3c1f7 100%); color: #6d28d9; }
.entry--life { background: var(--grain), linear-gradient(160deg, #d7f2ea 0%, #c7ebe0 70%, #b6e1d3 100%); color: #0f766e; }
.entry--other { background: var(--grain), linear-gradient(160deg, #e8edf2 0%, #dde4ec 70%, #d1dae4 100%); color: #475569; }

.entry--exercise,
.entry--work,
.entry--study,
.entry--life,
.entry--other {
  border: none;
  box-shadow: 0 7px 12px -8px rgba(76, 58, 20, 0.45), 0 2px 3px rgba(76, 58, 20, 0.14);
}

.entry--exercise:hover,
.entry--work:hover,
.entry--study:hover,
.entry--life:hover,
.entry--other:hover {
  box-shadow: 0 14px 20px -10px rgba(76, 58, 20, 0.5), 0 4px 8px rgba(76, 58, 20, 0.16);
}

/* 顶部居中的透明胶带（与任务便签同款） */
.entry::before {
  content: '';
  position: absolute;
  top: -9px;
  left: 50%;
  width: 56px;
  height: 20px;
  transform: translateX(-50%) rotate(-3deg);
  background:
    linear-gradient(105deg,
      transparent 0 18%,
      rgba(255, 255, 255, 0.55) 30%,
      rgba(255, 255, 255, 0.12) 42%,
      transparent 55% 68%,
      rgba(255, 255, 255, 0.4) 78%,
      transparent 90%),
    rgba(232, 240, 242, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.5);
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  clip-path: polygon(
    0% 15%, 3% 30%, 0% 45%, 3% 60%, 0% 75%, 2% 100%,
    98% 100%, 100% 75%, 97% 60%, 100% 45%, 97% 30%, 100% 15%,
    98% 0%, 2% 0%
  );
  filter: drop-shadow(0 2px 3px rgba(76, 58, 20, 0.16));
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
