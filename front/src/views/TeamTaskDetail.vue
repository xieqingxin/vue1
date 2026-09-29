<template>
  <div class="task-detail-page">
    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goBack">← 返回团队</button>
        <div class="brand">
          <span class="brand__dot" aria-hidden="true">🏷</span>
          <span>任务详情</span>
        </div>
      </div>
    </header>

    <main class="container">
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="loadError" class="state error">{{ loadError }}</div>

      <template v-else-if="task">
        <section class="card hero" :class="'accent-' + (task.type || 'other')">
          <div class="hero__badges">
            <span class="badge badge--type">{{ catLabel(task.type) }}</span>
            <span class="badge" :class="task.assigneeType === 'assigned' ? 'badge--assigned' : 'badge--all'">
              {{ task.assigneeType === 'assigned' ? '指定任务' : '全员任务' }}
            </span>
            <span class="badge" :class="task.overallStatus === 'completed' ? 'badge--done' : 'badge--doing'">
              {{ task.overallStatus === 'completed' ? '整体完成' : '进行中' }}
            </span>
          </div>
          <h1 class="hero__name">{{ task.name }}</h1>
          <p v-if="task.content" class="hero__content">{{ task.content }}</p>
          <div class="hero__reward">
            <span class="hero__reward-num">{{ task.reward }}</span>
            <span class="hero__reward-label">完成奖励</span>
          </div>
        </section>

        <!-- 完成情况 -->
        <section class="card">
          <h2 class="section-title">完成情况</h2>
          <div class="progress-row">
            <span class="progress-num">{{ task.completedCount }}<span class="progress-total"> / {{ task.total }} 人</span></span>
            <span class="badge" :class="task.myCompleted ? 'badge--done' : 'badge--doing'">
              {{ task.myCompleted ? '我已完成' : '我未完成' }}
            </span>
          </div>
          <div class="completion-list">
            <div v-for="c in task.completions" :key="c.userId" class="completion-item">
              <span class="member-avatar">{{ (c.nickname || c.username || '?').charAt(0) }}</span>
              <span class="completion-name">{{ c.nickname || c.username }}</span>
              <span class="completion-time">{{ formatTime(c.completeTime) }}</span>
            </div>
          </div>
          <p v-if="!task.completions.length" class="empty">还没有成员完成该任务。</p>
        </section>

        <!-- 字段明细 -->
        <section class="card">
          <h2 class="section-title">任务信息</h2>
          <dl class="fields">
            <div class="field"><dt>任务类型</dt><dd>{{ catLabel(task.type) }}</dd></div>
            <div class="field"><dt>指派方式</dt><dd>{{ task.assigneeType === 'assigned' ? '指定成员' : '全员' }}</dd></div>
            <div class="field"><dt>开始时间</dt><dd>{{ formatTime(task.startTime) }}</dd></div>
            <div class="field"><dt>截止时间</dt><dd>{{ formatTime(task.endTime) }}</dd></div>
            <div class="field"><dt>创建时间</dt><dd>{{ formatTime(task.createTime) }}</dd></div>
          </dl>
        </section>

        <!-- 操作区 -->
        <section class="actions">
          <button v-if="task.isLeader" class="btn btn--ghost" @click="openEdit">编辑任务</button>
          <button v-if="task.isLeader" class="btn btn--danger" @click="askDelete">删除任务</button>
          <button
            v-if="task.isLeader && task.overallStatus === 'ongoing'"
            class="btn btn--primary"
            @click="askOverallComplete"
          >标记整体完成</button>
          <button
            v-if="!task.myCompleted && !expired"
            class="btn btn--primary"
            @click="askComplete"
          >标记完成</button>
          <span v-if="task.myCompleted" class="done-hint">✓ 你已完成该任务</span>
        </section>
      </template>
    </main>

    <!-- 编辑任务对话框 -->
    <el-dialog v-model="showEdit" title="编辑任务" width="min(520px, 92vw)">
      <div class="dialog-form">
        <div class="field">
          <label>任务名</label>
          <input v-model.trim="editForm.name" class="input" maxlength="100" />
        </div>
        <div class="field">
          <label>任务内容</label>
          <textarea v-model.trim="editForm.content" class="input input--area" rows="3" maxlength="1000"></textarea>
        </div>
        <div class="field">
          <label>任务类型</label>
          <el-select v-model="editForm.type" class="w-full">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </div>
        <div class="field">
          <label>指派方式</label>
          <el-radio-group v-model="editForm.assigneeType">
            <el-radio-button label="all">全员</el-radio-button>
            <el-radio-button label="assigned">指定成员</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="editForm.assigneeType === 'assigned'" class="field">
          <label>选择成员</label>
          <el-select v-model="editForm.assigneeIds" multiple collapse-tags class="w-full">
            <el-option v-for="m in memberOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </div>
        <div class="field-grid">
          <div class="field">
            <label>开始时间</label>
            <el-date-picker v-model="editForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="w-full" />
          </div>
          <div class="field">
            <label>结束时间</label>
            <el-date-picker v-model="editForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="w-full" />
          </div>
        </div>
        <div class="field">
          <label>完成奖励</label>
          <input v-model.trim="editForm.reward" class="input" maxlength="100" />
        </div>
        <div v-if="editError" class="msg error">{{ editError }}</div>
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="showEdit = false">取消</button>
        <button class="btn btn--primary" :disabled="updating" @click="onUpdate">
          {{ updating ? '保存中...' : '保存' }}
        </button>
      </template>
    </el-dialog>

    <!-- 确认弹窗 -->
    <div v-if="pendingAction" class="modal-mask" @click.self="pendingAction = null">
      <div class="modal" role="dialog" aria-modal="true">
        <h3 class="modal__title">{{ pendingAction.title }}</h3>
        <p class="modal__text">{{ pendingAction.text }}</p>
        <div class="modal__actions">
          <button class="btn btn--ghost" @click="pendingAction = null">取消</button>
          <button class="btn btn--primary" @click="pendingAction.run">{{ pendingAction.confirm }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTeamTask, listMembers, updateTeamTask, deleteTeamTask,
  markTaskComplete, markTaskOverallComplete
} from '../api/team'

const route = useRoute()
const router = useRouter()
const teamId = computed(() => Number(route.params.teamId))
const taskId = computed(() => Number(route.params.taskId))

const categories = [
  { label: '锻炼', value: 'exercise' },
  { label: '工作', value: 'work' },
  { label: '学习', value: 'study' },
  { label: '生活', value: 'life' },
  { label: '其他', value: 'other' }
]

const task = ref(null)
const loading = ref(true)
const loadError = ref('')
const members = ref([])

const showEdit = ref(false)
const updating = ref(false)
const editError = ref('')
const editForm = ref({})

const pendingAction = ref(null)

const memberOptions = computed(() =>
  members.value.map((m) => ({ value: m.userId, label: m.nickname || m.username }))
)

const expired = computed(() => {
  if (!task.value || !task.value.endTime) return false
  return new Date(String(task.value.endTime).replace('T', ' ').replace(/-/g, '/')).getTime() < Date.now()
})

function catLabel(v) {
  return (categories.find((c) => c.value === v) || categories[4]).label
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function goBack() {
  router.push(`/teams/${teamId.value}`)
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [taskRes, memberRes] = await Promise.all([
      getTeamTask(teamId.value, taskId.value),
      listMembers(teamId.value)
    ])
    task.value = taskRes.data
    members.value = memberRes.data || []
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

function openEdit() {
  editError.value = ''
  editForm.value = {
    name: task.value.name,
    content: task.value.content || '',
    type: task.value.type,
    assigneeType: task.value.assigneeType,
    assigneeIds: task.value.assigneeIds || [],
    startTime: task.value.startTime,
    endTime: task.value.endTime,
    reward: task.value.reward
  }
  showEdit.value = true
}

async function onUpdate() {
  const f = editForm.value
  if (!f.name) return (editError.value = '请填写任务名')
  if (!f.startTime || !f.endTime) return (editError.value = '请选择开始和结束时间')
  if (!f.reward) return (editError.value = '请填写完成奖励')
  if (f.assigneeType === 'assigned' && (!f.assigneeIds || f.assigneeIds.length === 0)) {
    return (editError.value = '请选择指派成员')
  }
  updating.value = true
  editError.value = ''
  try {
    await updateTeamTask(teamId.value, taskId.value, {
      name: f.name,
      content: f.content,
      type: f.type,
      assigneeType: f.assigneeType,
      assigneeIds: f.assigneeType === 'assigned' ? f.assigneeIds : [],
      startTime: f.startTime,
      endTime: f.endTime,
      reward: f.reward
    })
    ElMessage.success('任务已更新')
    showEdit.value = false
    await load()
  } catch (e) {
    editError.value = e.message
  } finally {
    updating.value = false
  }
}

function askComplete() {
  pendingAction.value = {
    title: '确认完成任务',
    text: '标记完成后不可撤销，确认完成吗？',
    confirm: '确认完成',
    run: async () => {
      try {
        await markTaskComplete(teamId.value, taskId.value)
        ElMessage.success('已完成')
        pendingAction.value = null
        await load()
      } catch (e) {
        ElMessage.error(e.message)
      }
    }
  }
}

function askOverallComplete() {
  pendingAction.value = {
    title: '标记整体完成',
    text: '整体完成将单独显示，且不可回退；成员个人完成情况不受影响。',
    confirm: '确认',
    run: async () => {
      try {
        await markTaskOverallComplete(teamId.value, taskId.value)
        ElMessage.success('已标记整体完成')
        pendingAction.value = null
        await load()
      } catch (e) {
        ElMessage.error(e.message)
      }
    }
  }
}

function askDelete() {
  pendingAction.value = {
    title: '确认删除任务',
    text: '删除后任务不可见（软删除，完成记录保留），确认删除吗？',
    confirm: '删除',
    run: async () => {
      try {
        await deleteTeamTask(teamId.value, taskId.value)
        ElMessage.success('任务已删除')
        pendingAction.value = null
        router.push(`/teams/${teamId.value}`)
      } catch (e) {
        ElMessage.error(e.message)
      }
    }
  }
}

onMounted(load)
</script>

<style scoped>
.task-detail-page {
  min-height: 100vh;
  min-height: 100svh;
}

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
  background: var(--accent-soft);
  display: grid;
  place-items: center;
  font-size: 14px;
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

.container {
  position: relative;
  z-index: 1;
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 40px) clamp(16px, 4vw, 24px) 72px;
  display: flex;
  flex-direction: column;
  gap: 22px;
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

.card {
  background: var(--surface);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: clamp(20px, 3vw, 28px);
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.section-title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
  margin-bottom: 16px;
}

/* ---------- 英雄区 ---------- */
.hero {
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: var(--tc);
}

.hero.accent-exercise { --tc: #e8895a; }
.hero.accent-work { --tc: #5b8def; }
.hero.accent-study { --tc: #8b7af0; }
.hero.accent-life { --tc: #3bb88c; }
.hero.accent-other { --tc: #8a94a0; }

.hero__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.badge {
  font-size: 12px;
  font-weight: 650;
  padding: 4px 11px;
  border-radius: 999px;
}

.badge--type {
  background: var(--c-soft);
  color: var(--c);
}

.hero.accent-exercise { --c: #e8895a; --c-soft: #fbe9dc; }
.hero.accent-work { --c: #5b8def; --c-soft: #e6eefd; }
.hero.accent-study { --c: #8b7af0; --c-soft: #ece6fc; }
.hero.accent-life { --c: #3bb88c; --c-soft: #e1f5ec; }
.hero.accent-other { --c: #8a94a0; --c-soft: #eff2f5; }

.badge--all {
  background: #eef2f6;
  color: #64748b;
}

.badge--assigned {
  background: #f6ecdb;
  color: #bd8a4a;
}

.badge--doing {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.badge--done {
  background: var(--ok-soft);
  color: var(--ok);
}

.hero__name {
  font-family: var(--font-serif);
  font-size: clamp(22px, 3.4vw, 30px);
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 10px;
}

.hero__content {
  font-size: 15px;
  color: var(--ink-soft);
  line-height: 1.7;
}

.hero__reward {
  margin-top: 18px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.hero__reward-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--tc, var(--accent-deep));
}

.hero__reward-label {
  font-size: 13px;
  color: var(--ink-faint);
}

/* ---------- 完成情况 ---------- */
.progress-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.progress-num {
  font-size: 30px;
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.progress-total {
  font-size: 15px;
  font-weight: 500;
  color: var(--ink-faint);
}

.completion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.completion-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid var(--line);
  border-radius: 999px;
}

.member-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--ok-soft);
  color: var(--ok);
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
}

.completion-name {
  font-size: 13.5px;
  font-weight: 550;
  color: var(--ink);
}

.completion-time {
  font-size: 12px;
  color: var(--ink-faint);
}

/* ---------- 字段 ---------- */
.fields {
  margin: 0;
}

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.field:last-child {
  border-bottom: none;
}

.field dt {
  font-size: 13.5px;
  color: var(--ink-faint);
}

.field dd {
  font-size: 14px;
  font-weight: 550;
  color: var(--ink);
  text-align: right;
}

/* ---------- 操作 ---------- */
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
  color: var(--ink-soft);
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.btn:hover {
  border-color: var(--accent);
  color: var(--accent-deep);
  background: var(--accent-soft);
}

.btn--primary {
  border-color: rgba(121, 183, 166, 0.5);
  background: rgba(121, 183, 166, 0.9);
  color: #fff;
}

.btn--primary:hover {
  background: var(--accent);
  color: #fff;
}

.btn--danger {
  color: var(--danger);
  border-color: rgba(217, 119, 106, 0.22);
}

.btn--danger:hover {
  border-color: rgba(217, 119, 106, 0.4);
  background: var(--danger-soft);
  color: var(--danger);
}

.btn--ghost {
  background: rgba(255, 255, 255, 0.7);
}

.done-hint {
  display: inline-flex;
  align-items: center;
  color: var(--ok);
  font-size: 14px;
  font-weight: 600;
}

.empty {
  color: var(--ink-faint);
  font-size: 14px;
  text-align: center;
  padding: 20px 0;
}

/* ---------- 弹窗 ---------- */
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field {
  margin-bottom: 12px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-soft);
  margin-bottom: 6px;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-family: inherit;
  color: var(--ink);
  outline: none;
}

.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-soft);
}

.input--area {
  height: auto;
  padding: 10px 14px;
  resize: none;
  line-height: 1.6;
}

.w-full {
  width: 100%;
}

.msg {
  padding: 11px 14px;
  border-radius: 14px;
  font-size: 13px;
  margin-top: 6px;
}

.msg.error {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid rgba(217, 119, 106, 0.22);
}

/* ---------- 确认弹窗 ---------- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(96, 128, 118, 0.32);
  backdrop-filter: blur(3px);
  display: grid;
  place-items: center;
  padding: 20px;
}

.modal {
  width: min(400px, 100%);
  background: var(--surface-solid);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: 26px;
}

.modal__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 10px;
}

.modal__text {
  font-size: 14px;
  color: var(--ink-soft);
  line-height: 1.7;
}

.modal__actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

@media (max-width: 560px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>