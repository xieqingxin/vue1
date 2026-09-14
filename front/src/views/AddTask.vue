<template>
  <div class="add-page" :class="'page--' + type">
    <!-- 场景背景 -->
    <div class="scene" aria-hidden="true">
      <img class="scene__img" :src="sceneImg" alt="" />
      <span class="scene__scrim"></span>
    </div>

    <header class="topbar">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div class="page-title">
        <span class="page-title__icon">{{ meta.icon }}</span>
        <span>添加{{ meta.label }}任务</span>
      </div>
      <span class="topbar__spacer"></span>
    </header>

    <main class="container">
      <section class="sheet">
        <div v-if="error" class="msg error">{{ error }}</div>

        <form class="sticker-form form--paper" @submit.prevent="onCreate">
          <div class="sticker-field" style="--rot: -0.5deg">
            <label for="t-name">任务名</label>
            <input id="t-name" v-model.trim="form.name" type="text" placeholder="例如：整理季度报表" maxlength="100" />
          </div>

          <div class="sticker-field" style="--rot: 0.4deg">
            <label for="t-content">任务内容</label>
            <input id="t-content" v-model.trim="form.content" type="text" placeholder="简要描述要做的事（选填）" maxlength="1000" />
          </div>

          <div class="field-grid">
            <div class="sticker-field" style="--rot: 0.5deg">
              <label for="t-start">开始时间</label>
              <input id="t-start" v-model="form.startTime" type="datetime-local" />
            </div>
            <div class="sticker-field" style="--rot: -0.4deg">
              <label for="t-end">结束时间</label>
              <input id="t-end" v-model="form.endTime" type="datetime-local" />
            </div>
          </div>

          <div class="field-grid">
            <div class="sticker-field" style="--rot: 0.3deg">
              <label for="t-reward">完成奖励</label>
              <input id="t-reward" v-model.trim="form.reward" type="text" maxlength="100" placeholder="给自己定个奖励吧" />
            </div>
            <div class="sticker-field sticker-field--action" style="--rot: -0.3deg">
              <button class="btn-create" type="submit" :disabled="creating">
                {{ creating ? '提交中...' : '添加任务' }}
              </button>
            </div>
          </div>
        </form>
      </section>

      <!-- 对应类型的任务列表：便利贴墙（样式见 src/styles/note-wall.css） -->
      <section class="type-tasks">
        <div class="type-tasks__head">
          <h2 class="section-title">{{ meta.icon }} {{ meta.label }}任务</h2>
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
            :empty-text="`还没有${meta.label}任务，在上方添加一个吧。`"
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
import { useRoute, useRouter } from 'vue-router'
import { createTask, listTasks } from '../api/user'
import TaskList from '../components/TaskList.vue'

const route = useRoute()
const router = useRouter()

// 各类型场景背景：明亮的实景照片
const SCENE_IMGS = {
  exercise: 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=1920&q=80&auto=format&fit=crop', // 健身房
  work: 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=1920&q=80&auto=format&fit=crop', // 现代办公室
  study: 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=1920&q=80&auto=format&fit=crop', // 图书馆书架
  life: 'https://images.unsplash.com/photo-1556911220-bff31c812dba?w=1920&q=80&auto=format&fit=crop', // 明亮厨房
  other: 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=1920&q=80&auto=format&fit=crop' // 安静会议室
}

// 任务类型定义（与首页保持一致）
const TASK_TYPES = [
  { label: '锻炼', value: 'exercise', icon: '🏃' },
  { label: '工作', value: 'work', icon: '💼' },
  { label: '学习', value: 'study', icon: '📚' },
  { label: '生活', value: 'life', icon: '🏠' },
  { label: '其他', value: 'other', icon: '📌' }
]

const type = computed(() => {
  const t = route.params.type
  return TASK_TYPES.some((x) => x.value === t) ? t : 'other'
})

const meta = computed(() => TASK_TYPES.find((t) => t.value === type.value))

// 当前类型的场景背景图
const sceneImg = computed(() => SCENE_IMGS[type.value])

const form = ref({ name: '', content: '', startTime: '', endTime: '', reward: '' })
const creating = ref(false)
const error = ref('')

const tasks = ref([])
const filter = ref('all')
const listError = ref('')

const filters = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'doing' },
  { label: '已完成', value: 'done' },
  { label: '已过期', value: 'expired' }
]

// 当前类型下的任务，再按状态筛选
const visibleTasks = computed(() => {
  const list = tasks.value.filter((t) => (t.type || 'other') === type.value)
  if (filter.value === 'doing') return list.filter((t) => t.status === 0)
  if (filter.value === 'done') return list.filter((t) => t.status === 1)
  if (filter.value === 'expired') return list.filter((t) => t.status === 2)
  return list
})

async function loadTasks() {
  listError.value = ''
  try {
    const res = await listTasks()
    tasks.value = res.data || []
  } catch (e) {
    listError.value = e.message
  }
}

function goBack() {
  router.push('/home')
}

async function onCreate() {
  error.value = ''
  const f = form.value
  if (!f.name) {
    error.value = '请填写任务名'
    return
  }
  if (!f.startTime || !f.endTime) {
    error.value = '请选择开始时间和结束时间'
    return
  }
  if (new Date(f.endTime) <= new Date(f.startTime)) {
    error.value = '结束时间必须晚于开始时间'
    return
  }
  if (!f.reward) {
    error.value = '请填写完成奖励'
    return
  }
  creating.value = true
  try {
    await createTask({
      name: f.name,
      content: f.content,
      type: type.value,
      startTime: f.startTime.length === 16 ? f.startTime + ':00' : f.startTime,
      endTime: f.endTime.length === 16 ? f.endTime + ':00' : f.endTime,
      reward: f.reward
    })
    form.value = { name: '', content: '', startTime: '', endTime: '', reward: '' }
    await loadTasks()
  } catch (e) {
    error.value = e.message
  } finally {
    creating.value = false
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.add-page {
  min-height: 100vh;
  min-height: 100svh;
  background:
    radial-gradient(720px 320px at 85% -10%, rgba(22, 160, 133, 0.1), transparent 65%),
    radial-gradient(560px 280px at -5% 0%, rgba(125, 211, 252, 0.14), transparent 60%),
    var(--canvas);
}

/* 各类型的主题色 */
.page--exercise { --tc: #e2703a; }
.page--work { --tc: #2563eb; }
.page--study { --tc: #7c3aed; }
.page--life { --tc: #0d9488; }
.page--other { --tc: #64748b; }

/* 学习：米色横线纸（红色装订线 + 紫色横线） */
.page--study {
  --paper-border: #e6dfc8;
  --paper-surface:
    linear-gradient(90deg, transparent 0 26px, rgba(224, 138, 138, 0.5) 26px 27px, transparent 27px),
    repeating-linear-gradient(transparent 0 27px, rgba(124, 58, 237, 0.1) 27px 28px),
    #fffdf5;
  --paper-divider: rgba(124, 58, 237, 0.34);
  --paper-hover: rgba(124, 58, 237, 0.04);
  --paper-ink: #6d28d9;
  --paper-shadow: 4px 6px 0 rgba(23, 50, 44, 0.14);
  --paper-radius: 8px;
  --scene-bg: linear-gradient(150deg, #f3e7c9 0%, #e8d5a8 55%, #d9c08a 100%);
  --scrim-a: rgba(255, 251, 240, 0.32);
  --scrim-b: rgba(120, 96, 56, 0.22);
}

/* 工作：横线笔记本（白色纸面 + 蓝色横线 + 红色装订边线） */
.page--work {
  --paper-border: #d9e1ec;
  --paper-surface:
    linear-gradient(90deg, transparent 0 24px, rgba(220, 38, 38, 0.4) 24px 25px, transparent 25px 27px, rgba(220, 38, 38, 0.4) 27px 28px, transparent 28px),
    repeating-linear-gradient(transparent 0 27px, rgba(37, 99, 235, 0.14) 27px 28px),
    #fcfdff;
  --paper-divider: rgba(37, 99, 235, 0.32);
  --paper-hover: rgba(37, 99, 235, 0.04);
  --paper-ink: #1d4ed8;
  --paper-shadow: 0 10px 24px rgba(30, 58, 110, 0.12), 0 2px 6px rgba(30, 58, 110, 0.08);
  --paper-radius: 10px;
  --scene-bg: linear-gradient(150deg, #e8eef7 0%, #d5e0ef 55%, #c0cfe4 100%);
  --scrim-a: rgba(248, 251, 255, 0.36);
  --scrim-b: rgba(58, 82, 120, 0.24);
}

/* 锻炼：暖橙横线本 */
.page--exercise {
  --paper-border: #f0dcc9;
  --paper-surface:
    linear-gradient(90deg, transparent 0 24px, rgba(226, 112, 58, 0.45) 24px 25px, transparent 25px),
    repeating-linear-gradient(transparent 0 27px, rgba(226, 112, 58, 0.12) 27px 28px),
    #fffaf5;
  --paper-divider: rgba(226, 112, 58, 0.34);
  --paper-hover: rgba(226, 112, 58, 0.05);
  --paper-ink: #c2551d;
  --paper-shadow: 0 10px 24px rgba(120, 60, 20, 0.12), 0 2px 6px rgba(120, 60, 20, 0.08);
  --paper-radius: 10px;
  --scene-bg: linear-gradient(150deg, #fdeadd 0%, #f8d6bd 55%, #f0bd97 100%);
  --scrim-a: rgba(255, 248, 242, 0.34);
  --scrim-b: rgba(140, 80, 40, 0.22);
}

/* 生活：薄荷绿横线本 */
.page--life {
  --paper-border: #cfe5dd;
  --paper-surface:
    linear-gradient(90deg, transparent 0 24px, rgba(13, 148, 136, 0.4) 24px 25px, transparent 25px),
    repeating-linear-gradient(transparent 0 27px, rgba(13, 148, 136, 0.12) 27px 28px),
    #f7fdfb;
  --paper-divider: rgba(13, 148, 136, 0.32);
  --paper-hover: rgba(13, 148, 136, 0.05);
  --paper-ink: #0f766e;
  --paper-shadow: 0 10px 24px rgba(20, 80, 70, 0.12), 0 2px 6px rgba(20, 80, 70, 0.08);
  --paper-radius: 10px;
  --scene-bg: linear-gradient(150deg, #e6f4ef 0%, #cfe9e0 55%, #b5dccf 100%);
  --scrim-a: rgba(246, 253, 250, 0.36);
  --scrim-b: rgba(40, 90, 80, 0.22);
}

/* 其他：中性灰蓝横线本 */
.page--other {
  --paper-border: #dfe4ea;
  --paper-surface:
    linear-gradient(90deg, transparent 0 24px, rgba(100, 116, 139, 0.4) 24px 25px, transparent 25px),
    repeating-linear-gradient(transparent 0 27px, rgba(100, 116, 139, 0.12) 27px 28px),
    #fbfcfd;
  --paper-divider: rgba(100, 116, 139, 0.34);
  --paper-hover: rgba(100, 116, 139, 0.05);
  --paper-ink: #475569;
  --paper-shadow: 0 10px 24px rgba(50, 60, 75, 0.12), 0 2px 6px rgba(50, 60, 75, 0.08);
  --paper-radius: 10px;
  --scene-bg: linear-gradient(150deg, #eef1f4 0%, #dde3ea 55%, #c7d0da 100%);
  --scrim-a: rgba(250, 251, 252, 0.36);
  --scrim-b: rgba(70, 80, 95, 0.22);
}

/* ---------- 场景背景 ---------- */
.scene {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: var(--scene-bg);
}

.scene__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  animation: kenburns 26s ease-in-out infinite alternate;
}

/* 轻微提亮蒙层，保证纸张表单可读 */
.scene__scrim {
  position: absolute;
  inset: 0;
  background: radial-gradient(90% 80% at 50% 40%, var(--scrim-a) 0%, var(--scrim-b) 100%);
}

@keyframes kenburns {
  from { transform: scale(1.04); }
  to { transform: scale(1.12) translate3d(-1.5%, -1.5%, 0); }
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

.back-btn {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.18s, border-color 0.18s;
}

.back-btn:hover {
  color: var(--tc);
  border-color: var(--tc);
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 650;
  color: var(--ink);
}

.page-title__icon {
  font-size: 20px;
}

.topbar__spacer {
  width: 74px;
}

/* ---------- 布局 ---------- */
.container {
  position: relative;
  z-index: 1;
  max-width: 680px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 40px) clamp(16px, 4vw, 24px) 64px;
}

/* ---------- 表单容器 ---------- */
.sheet {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

/* ---------- 输入贴纸：与下方任务卡片同款表现 ---------- */
.sticker-form {
  position: relative;
}

.sticker-field {
  position: relative;
  background: var(--surface);
  border: 2px solid var(--tc);
  border-radius: 16px;
  padding: 12px 16px 14px;
  margin-bottom: 18px;
  box-shadow: 3px 4px 0 rgba(23, 50, 44, 0.1);
  transform: rotate(var(--rot, 0deg));
  transition: transform 0.18s, box-shadow 0.18s;
}

.sticker-field:hover {
  transform: rotate(0deg) translateY(-2px);
  box-shadow: 3px 5px 0 rgba(23, 50, 44, 0.14), var(--shadow-md);
}

.sticker-field:focus-within {
  transform: rotate(0deg) translateY(-2px);
  box-shadow: 3px 5px 0 rgba(23, 50, 44, 0.14), var(--shadow-md);
}

/* 贴纸角落的小胶带（与任务卡片一致） */
.sticker-field::before {
  content: '';
  position: absolute;
  top: -8px;
  right: 22px;
  width: 46px;
  height: 16px;
  background: rgba(255, 236, 153, 0.72);
  transform: rotate(4deg);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

/* 整个表单 = 一张纸（纸面样式由 --paper-surface 定义） */
.form--paper {
  border: 1px solid var(--paper-border);
  border-radius: var(--paper-radius);
  padding: 24px 22px 6px;
  background: var(--paper-surface);
  box-shadow: var(--paper-shadow);
}

/* 纸上的字段不再各自成卡片，只保留标签和输入 */
.form--paper .sticker-field {
  background: transparent;
  border: none;
  border-radius: 0;
  box-shadow: none;
  transform: none;
  padding: 2px 8px 10px 22px;
  margin-bottom: 12px;
}

.form--paper .sticker-field::before {
  display: none;
}

.form--paper .sticker-field:hover,
.form--paper .sticker-field:focus-within {
  transform: none;
  box-shadow: none;
}

.form--paper .sticker-field--action {
  padding: 2px 8px 14px 22px;
}

.form--paper .sticker-field label {
  color: var(--paper-ink);
}

.form--paper .btn-create {
  border-color: var(--tc);
  background: var(--tc);
}

.sticker-field label {
  display: block;
  font-size: 12.5px;
  font-weight: 650;
  letter-spacing: 0.04em;
  color: var(--tc);
  margin-bottom: 6px;
}

.sticker-field input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--ink);
}

.sticker-field input::placeholder {
  color: #a9bcb6;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

/* ---------- 提交按钮（贴纸样式） ---------- */
.sticker-field--action {
  display: flex;
  align-items: flex-end;
  padding: 10px 16px 12px;
}

.sticker-field--action::before {
  display: none;
}

.btn-create {
  width: 100%;
  height: 46px;
  border: 2px solid var(--tc);
  border-radius: 14px;
  background: var(--tc);
  color: #fff;
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 3px 4px 0 rgba(23, 50, 44, 0.18);
  transition: filter 0.18s, transform 0.1s, box-shadow 0.1s;
}

.btn-create:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn-create:active:not(:disabled) {
  transform: translate(2px, 3px);
  box-shadow: 0 0 0 rgba(23, 50, 44, 0.18);
}

.btn-create:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* ---------- 对应类型任务列表（便利贴墙见 src/styles/note-wall.css） ---------- */
.type-tasks {
  margin-top: 36px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.16s both;
}

.type-tasks__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 650;
  color: var(--ink);
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
  background: var(--tc);
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

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 560px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .topbar__spacer {
    width: 0;
  }

  .page-title {
    display: none;
  }
}
</style>
