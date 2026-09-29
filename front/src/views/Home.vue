<template>
  <div class="home">
    <!-- 顶部固定导航栏 -->
    <header class="navbar">
      <div class="navbar__inner">
        <button class="brand" @click="router.push('/home')">
          <span class="brand__dot" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          </span>
          <span class="brand__name">自律计划</span>
        </button>

        <nav class="nav">
          <button class="nav__link" :class="{ active: true }" @click="router.push('/home')">首页</button>
          <button class="nav__link" @click="router.push('/teams')">团队</button>
          <button class="nav__link" @click="router.push('/announcements')">公告</button>
          <button class="nav__link" @click="router.push('/about')">关于</button>
          <span class="nav__divider" aria-hidden="true"></span>
          <button class="nav__link" @click="router.push('/profile')">个人中心</button>
          <button class="nav__link nav__link--logout" @click="onLogout">退出登录</button>
        </nav>
      </div>
    </header>

    <main class="container">
      <!-- 1. 欢迎模块 -->
      <section class="welcome card">
        <div class="welcome__text">
          <h1>{{ greeting }}，{{ user ? user.nickname || user.username : '' }}</h1>
          <p>新的一天，从一件件小事开始。把目标拆成任务，每一步都算数。</p>
        </div>
        <div class="welcome__deco" aria-hidden="true">
          <svg viewBox="0 0 120 120" width="108" height="108" fill="none">
            <circle cx="60" cy="60" r="52" stroke="currentColor" stroke-width="1.5" opacity="0.35" />
            <circle cx="60" cy="60" r="40" stroke="currentColor" stroke-width="1.5" opacity="0.5" />
            <path d="M44 60l11 11 21-22" stroke="currentColor" stroke-width="7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
      </section>

      <!-- 2. 任务分类区 -->
      <section class="categories">
        <button
          v-for="tp in taskTypes"
          :key="tp.value"
          class="cat"
          :style="{ '--c': tp.color, '--c-soft': tp.soft }"
          @click="router.push(`/add/${tp.value}`)"
        >
          <span class="cat__icon" :class="'cat__icon--' + tp.value">
            <CategoryIcon :type="tp.value" />
          </span>
          <span class="cat__label">{{ tp.label }}</span>
          <span class="cat__add" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
          </span>
        </button>
      </section>

      <!-- 3. 筛选区 + 4. 任务列表 -->
      <section class="task-section">
        <div class="task-head">
          <h2 class="task-head__label">全部任务</h2>
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

        <TaskList
          :tasks="visibleTasks"
          :empty-text="tasks.length ? '该筛选条件下暂无任务' : '暂无任务，点击上方分类添加一个吧。'"
          @refresh="loadTasks"
          @error="(m) => (listError = m)"
        />
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
import { getProfile, listTasks } from '../api/user'
import { useUserStore } from '../store/user'
import TaskList from '../components/TaskList.vue'
import CategoryIcon from '../components/CategoryIcon.vue'

const router = useRouter()
const store = useUserStore()

const user = ref(store.user)
const tasks = ref([])
const filter = ref('all')
const listError = ref('')

// 任务类型定义（马卡龙色 + 分类图标）
const taskTypes = [
  { label: '锻炼', value: 'exercise', color: '#e8895a', soft: '#fbe9dc' },
  { label: '工作', value: 'work', color: '#5b8def', soft: '#e6eefd' },
  { label: '学习', value: 'study', color: '#8b7af0', soft: '#ece6fc' },
  { label: '生活', value: 'life', color: '#3bb88c', soft: '#e1f5ec' },
  { label: '其他', value: 'other', color: '#8a94a0', soft: '#eff2f5' }
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
/* ============ 背景：浅米 → 浅薄荷绿 柔和弥散渐变 + 极淡纹理 ============ */
.home {
  min-height: 100vh;
  min-height: 100svh;
  position: relative;
}

/* 极淡的噪点纹理点缀 */
.home::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='120' height='120' filter='url(%23n)' opacity='0.03'/%3E%3C/svg%3E");
}

/* ============ 顶部固定导航栏 ============ */
.navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-bottom: 1px solid var(--line);
}

.navbar__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 clamp(16px, 4vw, 28px);
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: none;
  cursor: pointer;
}

.brand__dot {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #8fc9b8, #79b8a6);
  color: #fff;
  display: grid;
  place-items: center;
  box-shadow: 0 8px 18px -8px rgba(79, 148, 138, 0.7);
}

.brand__name {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--ink);
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav__link {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 400;
  color: var(--ink-soft);
  padding: 8px 14px;
  border-radius: 999px;
  transition: color 0.18s, background 0.18s;
}

.nav__link:hover {
  color: var(--ink);
  background: rgba(120, 150, 140, 0.08);
}

.nav__link.active {
  color: var(--accent-deep);
  background: var(--accent-soft);
  font-weight: 500;
}

.nav__link--logout:hover {
  color: var(--danger);
  background: var(--danger-soft);
}

.nav__divider {
  width: 1px;
  height: 20px;
  margin: 0 8px;
  background: var(--line);
}

/* ============ 主内容区 ============ */
.container {
  position: relative;
  max-width: 1080px;
  margin: 0 auto;
  padding: 34px clamp(16px, 4vw, 28px) 72px;
  display: flex;
  flex-direction: column;
  gap: 26px;
}

/* 通用大白卡 */
.card {
  background: var(--surface);
  backdrop-filter: blur(20px) saturate(1.25);
  -webkit-backdrop-filter: blur(20px) saturate(1.25);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
}

/* 入场动效 */
.card,
.categories,
.task-section {
  animation: riseIn 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes riseIn {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: none; }
}

/* ============ 欢迎模块 ============ */
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 34px 38px;
  overflow: hidden;
}

.welcome h1 {
  font-family: var(--font-serif);
  font-size: clamp(28px, 3.4vw, 36px);
  font-weight: 600;
  letter-spacing: 0;
  color: var(--ink);
  margin-bottom: 12px;
}

.welcome p {
  font-size: 15px;
  color: var(--ink-soft);
  font-weight: 300;
  line-height: 1.7;
}

.welcome__deco {
  color: var(--accent-deep);
  opacity: 0.9;
  flex-shrink: 0;
}

/* ============ 任务分类区 ============ */
.categories {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.cat {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 20px 18px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 22px;
  cursor: pointer;
  background: var(--c-soft);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  text-align: left;
  transition: transform 0.2s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.2s;
}

.cat:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 30px -22px rgba(96, 128, 118, 0.5);
}

.cat__icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--c);
  display: grid;
  place-items: center;
  box-shadow: 0 6px 14px -8px rgba(96, 128, 118, 0.35);
}

.cat__icon :deep(svg) {
  width: 24px;
  height: 24px;
}

.cat__label {
  font-size: 15px;
  font-weight: 500;
  color: var(--ink);
}

.cat__add {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  color: var(--c);
  display: grid;
  place-items: center;
  box-shadow: 0 4px 10px -4px rgba(96, 128, 118, 0.35);
  transition: color 0.18s, background 0.18s;
}

.cat:hover .cat__add {
  background: var(--c);
  color: #ffffff;
}

/* ============ 筛选区 + 任务列表 ============ */
.task-section {
  animation-delay: 0.12s;
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.task-head__label {
  font-size: 15px;
  font-weight: 500;
  color: var(--ink);
  padding-left: 14px;
  border-left: 4px solid var(--accent);
  line-height: 1;
}

.filters {
  display: inline-flex;
  gap: 6px;
  background: var(--surface);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 999px;
  padding: 5px;
  box-shadow: var(--shadow-sm);
}

.filter-btn {
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 400;
  color: var(--ink-soft);
  padding: 7px 18px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}

.filter-btn:hover {
  color: var(--ink);
}

.filter-btn.active {
  background: var(--accent);
  color: #fff;
  font-weight: 500;
  box-shadow: 0 6px 14px -8px rgba(79, 148, 138, 0.7);
}

.msg {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 13.5px;
  margin-bottom: 16px;
  animation: riseIn 0.28s ease both;
}

.msg.error {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid rgba(217, 119, 106, 0.22);
}

@media (max-width: 860px) {
  .categories {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .categories {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .welcome__deco {
    display: none;
  }

  .nav__divider {
    display: none;
  }
}
</style>