<template>
  <div class="profile-page">
    <!-- 场景背景：与主页/添加页同款（实景照片 + 提亮蒙层 + Ken Burns） -->
    <div class="scene" aria-hidden="true">
      <img class="scene__img" :src="sceneImg" alt="" />
      <span class="scene__scrim"></span>
    </div>

    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goBack">← 返回任务列表</button>
        <div class="brand">
          <span class="brand__dot" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          </span>
          <span>自律计划任务平台</span>
        </div>
      </div>
    </header>

    <main class="container">
      <!-- 个人档案 -->
      <section class="profile-hero">
        <span class="profile-hero__avatar" aria-hidden="true">
          <img v-if="user && user.avatar" :src="user.avatar" alt="avatar" />
          <template v-else>{{ initial }}</template>
        </span>
        <div class="profile-hero__info">
          <h1 class="profile-hero__name">{{ user ? user.nickname || user.username : '' }}</h1>
          <p class="profile-hero__meta">
            <span>@{{ user ? user.username : '' }}</span>
            <span v-if="user && user.email" class="dot">·</span>
            <span v-if="user && user.email">{{ user.email }}</span>
          </p>
          <p class="profile-hero__joined">加入于 {{ formatDate(user ? user.createdAt : '') }}</p>
        </div>
      </section>

      <!-- 任务统计 -->
      <section class="stats">
        <div class="stat">
          <span class="stat__num">{{ tasks.length }}</span>
          <span class="stat__label">全部任务</span>
        </div>
        <div class="stat">
          <span class="stat__num stat__num--doing">{{ doingCount }}</span>
          <span class="stat__label">进行中</span>
        </div>
        <div class="stat">
          <span class="stat__num stat__num--done">{{ doneCount }}</span>
          <span class="stat__label">已完成</span>
        </div>
        <div class="stat">
          <span class="stat__num stat__num--expired">{{ expiredCount }}</span>
          <span class="stat__label">已过期</span>
        </div>
      </section>

      <!-- 完成日历 -->
      <section class="calendar">
        <div class="calendar__head">
          <h2 class="calendar__title">任务完成日历</h2>
          <div class="calendar__nav">
            <button class="cal-nav-btn" aria-label="上一月" @click="prevMonth">‹</button>
            <span class="calendar__month">{{ year }} 年 {{ month + 1 }} 月</span>
            <button class="cal-nav-btn" aria-label="下一月" @click="nextMonth">›</button>
          </div>
        </div>
        <div class="calendar__legend">
          <span class="legend-item"><span class="legend-flower">🌸</span>当天有任务完成</span>
          <span class="legend-count" v-if="monthDoneCount">本月完成 {{ monthDoneCount }} 项</span>
        </div>
        <div class="calendar__grid">
          <span v-for="w in WEEKDAYS" :key="w" class="calendar__weekday">{{ w }}</span>
          <div
            v-for="cell in cells"
            :key="cell.key"
            class="calendar__cell"
            :class="{ blank: !cell.day, today: cell.isToday, 'has-flower': cell.done > 0 }"
          >
            <template v-if="cell.day">
              <span class="calendar__day">{{ cell.day }}</span>
              <span v-if="cell.done > 0" class="calendar__flower" :title="'完成 ' + cell.done + ' 项任务'">🌸</span>
              <span v-else class="calendar__flower calendar__flower--ghost"></span>
            </template>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listTasks } from '../api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()

const user = ref(store.user)
const tasks = ref([])
const loadError = ref('')

// 个人中心场景背景：明亮书桌俯拍实景
const sceneImg = 'https://images.unsplash.com/photo-1521737711867-e3b97375f902?w=1920&q=80&auto=format&fit=crop'

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

const today = new Date()
const year = ref(today.getFullYear())
const month = ref(today.getMonth())

const initial = computed(() => {
  const name = user.value ? user.value.nickname || user.value.username : ''
  return name ? name.charAt(0).toUpperCase() : '?'
})

const doingCount = computed(() => tasks.value.filter((t) => t.status === 0).length)
const doneCount = computed(() => tasks.value.filter((t) => t.status === 1).length)
const expiredCount = computed(() => tasks.value.filter((t) => t.status === 2).length)

// 按日期聚合完成数量：'YYYY-MM-DD' -> count
const doneByDate = computed(() => {
  const map = {}
  for (const t of tasks.value) {
    if (t.status === 1 && t.completedAt) {
      const key = String(t.completedAt).slice(0, 10)
      map[key] = (map[key] || 0) + 1
    }
  }
  return map
})

function pad(n) {
  return String(n).padStart(2, '0')
}

const cells = computed(() => {
  const y = year.value
  const m = month.value
  const firstWeekday = new Date(y, m, 1).getDay()
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const list = []
  for (let i = 0; i < firstWeekday; i++) {
    list.push({ key: `b${i}`, day: 0, done: 0, isToday: false })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const key = `${y}-${pad(m + 1)}-${pad(d)}`
    list.push({
      key,
      day: d,
      done: doneByDate.value[key] || 0,
      isToday: y === today.getFullYear() && m === today.getMonth() && d === today.getDate()
    })
  }
  return list
})

const monthDoneCount = computed(() => cells.value.reduce((s, c) => s + c.done, 0))

function prevMonth() {
  if (month.value === 0) {
    month.value = 11
    year.value -= 1
  } else {
    month.value -= 1
  }
}

function nextMonth() {
  if (month.value === 11) {
    month.value = 0
    year.value += 1
  } else {
    month.value += 1
  }
}

function formatDate(t) {
  if (!t) return '-'
  return String(t).slice(0, 10).replace(/-/g, '.')
}

function goBack() {
  router.push('/home')
}

async function load() {
  loadError.value = ''
  try {
    const res = await listTasks()
    tasks.value = res.data || []
  } catch (e) {
    loadError.value = e.message
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  min-height: 100svh;
  background:
    radial-gradient(720px 320px at 85% -10%, rgba(22, 160, 133, 0.1), transparent 65%),
    radial-gradient(560px 280px at -5% 0%, rgba(125, 211, 252, 0.14), transparent 60%),
    var(--canvas);
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
  position: relative;
  z-index: 1;
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 44px) clamp(16px, 4vw, 24px) 72px;
}

/* ---------- 个人档案 ---------- */
.profile-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: clamp(22px, 3.5vw, 32px);
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.05s both;
}

.profile-hero__avatar {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--accent-soft);
  color: var(--accent-deep);
  display: grid;
  place-items: center;
  font-size: 30px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px var(--line);
}

.profile-hero__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-hero__info {
  min-width: 0;
}

.profile-hero__name {
  font-size: clamp(20px, 3vw, 26px);
  font-weight: 650;
  letter-spacing: -0.015em;
  margin-bottom: 5px;
}

.profile-hero__meta {
  font-size: 13.5px;
  color: var(--ink-soft);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-hero__meta .dot {
  color: #cbd5d1;
}

.profile-hero__joined {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--ink-faint);
}

/* ---------- 任务统计 ---------- */
.stats {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 18px 8px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.12s both;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  border-right: 1px solid var(--line);
}

.stat:last-child {
  border-right: none;
}

.stat__num {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.stat__num--doing {
  color: var(--accent-deep);
}

.stat__num--done {
  color: #64748b;
}

.stat__num--expired {
  color: #dc2626;
}

.stat__label {
  font-size: 12.5px;
  color: var(--ink-faint);
}

/* ---------- 完成日历 ---------- */
.calendar {
  margin-top: 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 20px clamp(16px, 3vw, 26px) 22px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.19s both;
}

.calendar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.calendar__title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
}

.calendar__nav {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.cal-nav-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: border-color 0.18s, color 0.18s, background 0.18s;
}

.cal-nav-btn:hover {
  border-color: var(--accent);
  color: var(--accent-deep);
  background: var(--accent-soft);
}

.calendar__month {
  font-size: 14.5px;
  font-weight: 650;
  color: var(--ink);
  min-width: 92px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.calendar__legend {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12.5px;
  color: var(--ink-faint);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-flower {
  font-size: 13px;
  line-height: 1;
}

.legend-count {
  color: var(--accent-deep);
  font-weight: 600;
}

.calendar__grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.calendar__weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-faint);
  padding: 4px 0;
}

.calendar__cell {
  aspect-ratio: 1 / 1;
  border-radius: 12px;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  transition: background 0.18s, border-color 0.18s;
}

.calendar__cell.blank {
  border: none;
}

.calendar__cell:not(.blank):hover {
  background: var(--canvas);
  border-color: var(--line);
}

.calendar__cell.today {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.calendar__cell.has-flower {
  background: rgba(226, 112, 58, 0.05);
}

.calendar__cell.has-flower.today {
  background: var(--accent-soft);
}

.calendar__day {
  font-size: 13.5px;
  font-weight: 550;
  color: var(--ink-soft);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.calendar__cell.today .calendar__day {
  color: var(--accent-deep);
  font-weight: 700;
}

.calendar__flower {
  font-size: 13px;
  line-height: 1;
}

.calendar__flower--ghost {
  visibility: hidden;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

@media (max-width: 560px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px 0;
    row-gap: 14px;
  }

  .stat:nth-child(2) {
    border-right: none;
  }

  .profile-hero {
    gap: 14px;
  }

  .profile-hero__avatar {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }
}
</style>
