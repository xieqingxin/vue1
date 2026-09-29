<template>
  <div class="add-page" :class="'page--' + type">
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

        <form class="task-form" @submit.prevent="onCreate">
          <div class="field">
            <label for="t-name">任务名</label>
            <input id="t-name" v-model.trim="form.name" type="text" placeholder="例如：整理季度报表" maxlength="100" />
          </div>

          <div class="field">
            <label for="t-content">任务内容</label>
            <textarea id="t-content" v-model.trim="form.content" rows="3" maxlength="1000" placeholder="简要描述要做的事（选填）"></textarea>
          </div>

          <div class="field-grid">
            <div class="field">
              <label for="t-start">开始时间</label>
              <el-date-picker
                id="t-start"
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placement="bottom-start"
                :fallback-placements="['bottom-start', 'bottom']"
                :clearable="true"
              />
            </div>
            <div class="field">
              <label for="t-end">结束时间</label>
              <el-date-picker
                id="t-end"
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placement="bottom-start"
                :fallback-placements="['bottom-start', 'bottom']"
                :clearable="true"
              />
            </div>
          </div>

          <div class="field-grid">
            <div class="field">
              <label for="t-reward">完成奖励</label>
              <input id="t-reward" v-model.trim="form.reward" type="text" maxlength="100" placeholder="给自己定个奖励吧" />
            </div>
            <div class="field field--action">
              <button class="btn-create" type="submit" :disabled="creating">
                {{ creating ? '提交中...' : '添加任务' }}
              </button>
            </div>
          </div>
        </form>
      </section>

      <!-- 对应类型的任务列表 -->
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

        <TaskList
          :tasks="visibleTasks"
          :empty-text="`还没有${meta.label}任务，在上方添加一个吧。`"
          @refresh="loadTasks"
          @error="(m) => (listError = m)"
        />
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

const form = ref({ name: '', content: '', startTime: null, endTime: null, reward: '' })
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
      startTime: f.startTime,
      endTime: f.endTime,
      reward: f.reward
    })
    form.value = { name: '', content: '', startTime: null, endTime: null, reward: '' }
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
}

/* 各类型主题色（用于标题点缀与提交按钮） */
.page--exercise { --tc: #e8895a; }
.page--work { --tc: #5b8def; }
.page--study { --tc: #8b7af0; }
.page--life { --tc: #3bb88c; }
.page--other { --tc: #8a94a0; }

/* ---------- 顶栏 ---------- */
.topbar {
  height: 62px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  border-bottom: 1px solid rgba(23, 50, 44, 0.06);
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
  background: rgba(255, 255, 255, 0.6);
  color: var(--ink-soft);
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.18s, border-color 0.18s;
}

.back-btn:hover {
  color: var(--tc, var(--accent-deep));
  border-color: var(--tc, var(--accent));
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
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
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 40px) clamp(16px, 4vw, 24px) 64px;
}

/* ---------- 表单容器 ---------- */
.sheet {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.task-form {
  background: var(--surface);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 26px;
  box-shadow: var(--shadow-md);
  padding: clamp(20px, 3vw, 30px);
}

.field {
  margin-bottom: 18px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-soft);
  margin-bottom: 7px;
}

.field input,
.field textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-family: inherit;
  font-weight: 400;
  color: var(--ink);
  outline: none;
  padding: 0 14px;
  transition: border-color 0.18s, box-shadow 0.18s;
}

.field input {
  height: 46px;
}

.field textarea {
  min-height: 84px;
  padding: 12px 14px;
  line-height: 1.6;
  resize: none;
  overflow-y: auto;
}

.field input::placeholder,
.field textarea::placeholder {
  color: #b7c6c0;
}

.field input:hover,
.field textarea:hover {
  border-color: rgba(120, 150, 140, 0.28);
}

.field input:focus,
.field textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-soft);
  background: #ffffff;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.field--action {
  display: flex;
  align-items: flex-end;
}

/* Element Plus 日期时间选择器融入浅色表单 */
.task-form :deep(.el-date-editor) {
  width: 100%;
  --el-input-placeholder-color: #b7c6c0;
}

.task-form :deep(.el-input__wrapper) {
  border-radius: 13px;
}

/* ---------- 提交按钮：扁平化 ---------- */
.btn-create {
  width: 100%;
  height: 46px;
  border: 1px solid rgba(121, 183, 166, 0.5);
  border-radius: 999px;
  background: rgba(121, 183, 166, 0.9);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 14px 28px -16px rgba(79, 148, 138, 0.7);
  transition: background 0.18s, transform 0.18s;
}

.btn-create:hover:not(:disabled) {
  background: var(--accent);
  transform: translateY(-1px);
}

.btn-create:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* ---------- 对应类型任务列表 ---------- */
.type-tasks {
  margin-top: 30px;
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
  padding-left: 12px;
  border-left: 4px solid var(--tc, var(--accent));
  line-height: 1;
}

.filters {
  display: inline-flex;
  gap: 6px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(23, 50, 44, 0.06);
  border-radius: 999px;
  padding: 5px;
}

.filter-btn {
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 550;
  color: var(--ink-soft);
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}

.filter-btn:hover {
  color: var(--ink);
}

.filter-btn.active {
  background: var(--tc, var(--accent));
  color: #fff;
  font-weight: 650;
}

.msg {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 13.5px;
  margin-bottom: 16px;
  animation: rise 0.28s ease both;
}

.msg.error {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid rgba(217, 119, 106, 0.22);
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

  .field--action {
    align-items: stretch;
  }

  .topbar__spacer {
    width: 0;
  }

  .page-title {
    display: none;
  }
}
</style>