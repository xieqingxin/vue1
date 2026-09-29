<template>
  <div class="team-detail-page">
    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goTeams">← 返回团队</button>
        <div class="brand">
          <span class="brand__dot" aria-hidden="true">{{ (team && team.name ? team.name.charAt(0) : '团') }}</span>
          <span>{{ team ? team.name : '团队详情' }}</span>
        </div>
      </div>
    </header>

    <main class="container">
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="loadError" class="state error">{{ loadError }}</div>

      <template v-else>
        <!-- 团队信息 -->
        <section class="card hero">
          <div class="hero__tags">
            <span class="cat-tag" :style="{ color: catColor(team.category), background: catSoft(team.category) }">
              {{ catLabel(team.category) }}
            </span>
            <span class="badge" :class="isLeader ? 'badge--leader' : 'badge--member'">
              {{ isLeader ? '队长' : '成员' }}
            </span>
          </div>
          <h1 class="hero__name">{{ team.name }}</h1>
          <p class="hero__meta">成员 {{ team.memberCount }}/{{ team.maxSize }} · 队长 ID {{ team.leaderId }} · 创建于 {{ formatTime(team.createTime) }}</p>

          <div class="hero__actions">
            <button v-if="isLeader" class="btn" @click="openCreate">+ 创建任务</button>
            <button v-if="isLeader" class="btn" @click="openTransfer">转让队长</button>
            <button v-if="isLeader" class="btn btn--danger" @click="askDissolve">解散团队</button>
            <button v-if="!isLeader" class="btn btn--danger" @click="askQuit">退出团队</button>
          </div>
        </section>

        <!-- 待审核申请（队长） -->
        <section v-if="isLeader" class="card">
          <h2 class="section-title">待审核申请</h2>
          <div v-if="applications.length" class="app-list">
            <div v-for="a in applications" :key="a.userId" class="app-item">
              <span class="app-item__name">{{ a.nickname || a.username }}</span>
              <span class="app-item__time">{{ formatTime(a.joinTime) }}</span>
              <div class="app-item__actions">
                <button class="btn btn--sm" @click="review(a, true)">通过</button>
                <button class="btn btn--sm btn--ghost" @click="review(a, false)">拒绝</button>
              </div>
            </div>
          </div>
          <p v-else class="empty">暂无待审核申请。</p>
        </section>

        <!-- 成员列表 -->
        <section class="card">
          <h2 class="section-title">团队成员</h2>
          <div class="member-grid">
            <div v-for="m in members" :key="m.userId" class="member">
              <span class="member__avatar">{{ (m.nickname || m.username || '?').charAt(0) }}</span>
              <span class="member__name">{{ m.nickname || m.username }}</span>
              <span class="badge" :class="m.role === 'leader' ? 'badge--leader' : 'badge--member'">
                {{ m.role === 'leader' ? '队长' : '成员' }}
              </span>
              <div v-if="isLeader && m.role !== 'leader'" class="member__ops">
                <button class="linklike" @click="openTransferTo(m)">转让</button>
                <button class="linklike linklike--danger" @click="askKick(m)">踢出</button>
              </div>
            </div>
          </div>
        </section>

        <!-- 团队任务 -->
        <section class="card">
          <h2 class="section-title">团队任务</h2>
          <div v-if="tasks.length" class="task-list">
            <button
              v-for="t in tasks"
              :key="t.id"
              class="task-card"
              :class="'type-' + (t.type || 'other')"
              @click="router.push(`/teams/${teamId}/tasks/${t.id}`)"
            >
              <div class="task-card__head">
                <span class="tag" :class="t.assigneeType === 'assigned' ? 'tag--assigned' : 'tag--all'">
                  {{ t.assigneeType === 'assigned' ? '指派' : '全员' }}
                </span>
                <span class="task-type-tag" :class="'tt-' + (t.type || 'other')">{{ catLabel(t.type) }}</span>
                <span v-if="t.overallStatus === 'completed'" class="tag tag--done">整体完成</span>
              </div>
              <h3 class="task-card__name">{{ t.name }}</h3>
              <p v-if="t.content" class="task-card__content">{{ t.content }}</p>
              <div class="task-card__meta">
                <span>进度 {{ t.completedCount }}/{{ t.total }}</span>
                <span class="dot">·</span>
                <span>止 {{ formatTime(t.endTime) }}</span>
                <span class="dot">·</span>
                <span>奖励 {{ t.reward }}</span>
              </div>
            </button>
          </div>
          <p v-else class="empty">暂无团队任务。</p>
        </section>
      </template>
    </main>

    <!-- 创建任务对话框 -->
    <el-dialog v-model="showCreate" title="创建团队任务" width="min(520px, 92vw)">
      <div class="dialog-form">
        <div class="field">
          <label>任务名</label>
          <input v-model.trim="taskForm.name" class="input" maxlength="100" placeholder="例如：团队周报" />
        </div>
        <div class="field">
          <label>任务内容</label>
          <textarea v-model.trim="taskForm.content" class="input input--area" rows="3" maxlength="1000" placeholder="简要描述（选填）"></textarea>
        </div>
        <div class="field">
          <label>任务类型</label>
          <el-select v-model="taskForm.type" class="w-full">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </div>
        <div class="field">
          <label>指派方式</label>
          <el-radio-group v-model="taskForm.assigneeType">
            <el-radio-button label="all">全员</el-radio-button>
            <el-radio-button label="assigned">指定成员</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="taskForm.assigneeType === 'assigned'" class="field">
          <label>选择成员</label>
          <el-select v-model="taskForm.assigneeIds" multiple collapse-tags class="w-full" placeholder="选择指派成员">
            <el-option
              v-for="m in memberOptions"
              :key="m.value"
              :label="m.label"
              :value="m.value"
            />
          </el-select>
        </div>
        <div class="field-grid">
          <div class="field">
            <label>开始时间</label>
            <el-date-picker v-model="taskForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择开始时间" class="w-full" />
          </div>
          <div class="field">
            <label>结束时间</label>
            <el-date-picker v-model="taskForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择结束时间" class="w-full" />
          </div>
        </div>
        <div class="field">
          <label>完成奖励</label>
          <input v-model.trim="taskForm.reward" class="input" maxlength="100" placeholder="例如：团队聚餐" />
        </div>
        <div v-if="taskError" class="msg error">{{ taskError }}</div>
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="showCreate = false">取消</button>
        <button class="btn btn--primary" :disabled="creatingTask" @click="onCreateTask">
          {{ creatingTask ? '提交中...' : '创建' }}
        </button>
      </template>
    </el-dialog>

    <!-- 转让队长对话框 -->
    <el-dialog v-model="showTransfer" title="转让队长" width="min(420px, 92vw)">
      <p class="dialog-tip">选择要转让的成员，转让后你将降为普通成员。</p>
      <el-select v-model="transferTarget" class="w-full" placeholder="选择新队长">
        <el-option v-for="m in memberOptions" :key="m.value" :label="m.label" :value="m.value" />
      </el-select>
      <template #footer>
        <button class="btn btn--ghost" @click="showTransfer = false">取消</button>
        <button class="btn btn--primary" @click="onTransfer">确认转让</button>
      </template>
    </el-dialog>

    <!-- 通用确认弹窗 -->
    <div v-if="pendingAction" class="modal-mask" @click.self="pendingAction = null">
      <div class="modal" role="dialog" aria-modal="true">
        <h3 class="modal__title">{{ pendingAction.title }}</h3>
        <p class="modal__text">{{ pendingAction.text }}</p>
        <div class="modal__actions">
          <button class="btn btn--ghost" @click="pendingAction = null">取消</button>
          <button class="btn btn--danger-solid" @click="pendingAction.run">{{ pendingAction.confirm }}</button>
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
  getTeamDetail, listMembers, listApplications, listTeamTasks, createTeamTask,
  reviewApplication, kickMember, quitTeam, transferLeader, dissolveTeam
} from '../api/team'

const route = useRoute()
const router = useRouter()
const teamId = computed(() => Number(route.params.id))

const categories = [
  { label: '锻炼', value: 'exercise' },
  { label: '工作', value: 'work' },
  { label: '学习', value: 'study' },
  { label: '生活', value: 'life' },
  { label: '其他', value: 'other' }
]

const team = ref(null)
const members = ref([])
const applications = ref([])
const tasks = ref([])
const loading = ref(true)
const loadError = ref('')

const isLeader = computed(() => team.value && team.value.myRole === 'leader')

const memberOptions = computed(() =>
  members.value
    .filter((m) => m.role !== 'leader')
    .map((m) => ({ value: m.userId, label: m.nickname || m.username }))
)

// 创建任务表单
const showCreate = ref(false)
const creatingTask = ref(false)
const taskError = ref('')
const taskForm = ref({ name: '', content: '', type: 'other', assigneeType: 'all', assigneeIds: [], startTime: null, endTime: null, reward: '' })

const showTransfer = ref(false)
const transferTarget = ref(null)

const pendingAction = ref(null)

function catLabel(v) {
  return (categories.find((c) => c.value === v) || categories[4]).label
}

function catColor(v) {
  const map = { exercise: '#e8895a', work: '#5b8def', study: '#8b7af0', life: '#3bb88c', other: '#8a94a0' }
  return map[v] || map.other
}

function catSoft(v) {
  const map = { exercise: '#fbe9dc', work: '#e6eefd', study: '#ece6fc', life: '#e1f5ec', other: '#eff2f5' }
  return map[v] || map.other
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function goTeams() {
  router.push('/teams')
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [detailRes, memberRes, taskRes] = await Promise.all([
      getTeamDetail(teamId.value),
      listMembers(teamId.value),
      listTeamTasks(teamId.value)
    ])
    team.value = detailRes.data
    members.value = memberRes.data || []
    tasks.value = taskRes.data || []
    if (team.value.myRole === 'leader') {
      const appRes = await listApplications(teamId.value)
      applications.value = appRes.data || []
    }
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  taskError.value = ''
  taskForm.value = { name: '', content: '', type: 'other', assigneeType: 'all', assigneeIds: [], startTime: null, endTime: null, reward: '' }
  showCreate.value = true
}

async function onCreateTask() {
  const f = taskForm.value
  if (!f.name) return (taskError.value = '请填写任务名')
  if (!f.startTime || !f.endTime) return (taskError.value = '请选择开始和结束时间')
  if (!f.reward) return (taskError.value = '请填写完成奖励')
  if (f.assigneeType === 'assigned' && (!f.assigneeIds || f.assigneeIds.length === 0)) {
    return (taskError.value = '请选择指派成员')
  }
  creatingTask.value = true
  taskError.value = ''
  try {
    await createTeamTask(teamId.value, {
      name: f.name,
      content: f.content,
      type: f.type,
      assigneeType: f.assigneeType,
      assigneeIds: f.assigneeType === 'assigned' ? f.assigneeIds : [],
      startTime: f.startTime,
      endTime: f.endTime,
      reward: f.reward
    })
    ElMessage.success('任务创建成功')
    showCreate.value = false
    const res = await listTeamTasks(teamId.value)
    tasks.value = res.data || []
  } catch (e) {
    taskError.value = e.message
  } finally {
    creatingTask.value = false
  }
}

function openTransfer() {
  transferTarget.value = null
  showTransfer.value = true
}

function openTransferTo(m) {
  transferTarget.value = m.userId
  showTransfer.value = true
}

async function onTransfer() {
  if (!transferTarget.value) return
  try {
    await transferLeader(teamId.value, transferTarget.value)
    ElMessage.success('队长已转让')
    showTransfer.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function review(a, approve) {
  try {
    await reviewApplication(teamId.value, a.userId, approve)
    ElMessage.success(approve ? '已通过' : '已拒绝')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function askKick(m) {
  pendingAction.value = {
    title: '确认移出成员',
    text: `确定将「${m.nickname || m.username}」移出团队吗？其在本团队的完成记录将被删除。`,
    confirm: '移出',
    run: async () => {
      try {
        await kickMember(teamId.value, m.userId)
        ElMessage.success('已移出成员')
        pendingAction.value = null
        await load()
      } catch (e) {
        ElMessage.error(e.message)
      }
    }
  }
}

function askQuit() {
  pendingAction.value = {
    title: '确认退出团队',
    text: '退出后你将无法查看该团队任务，且在本团队的完成记录会被删除。',
    confirm: '退出',
    run: async () => {
      try {
        await quitTeam(teamId.value)
        ElMessage.success('已退出团队')
        pendingAction.value = null
        router.push('/teams')
      } catch (e) {
        ElMessage.error(e.message)
      }
    }
  }
}

function askDissolve() {
  pendingAction.value = {
    title: '确认解散团队',
    text: '解散后团队不可恢复，但成员与完成记录会保留。',
    confirm: '解散',
    run: async () => {
      try {
        await dissolveTeam(teamId.value)
        ElMessage.success('团队已解散')
        pendingAction.value = null
        router.push('/teams')
      } catch (e) {
        ElMessage.error(e.message)
      }
    }
  }
}

onMounted(load)
</script>

<style scoped>
.team-detail-page {
  min-height: 100vh;
  min-height: 100svh;
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
  max-width: 760px;
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
  font-size: 14px;
  font-weight: 650;
  color: var(--ink-faint);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin-bottom: 16px;
}

/* ---------- 英雄区 ---------- */
.hero__tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.cat-tag {
  font-size: 12.5px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 999px;
}

.badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 999px;
}

.badge--leader {
  background: #f6ecdb;
  color: #bd8a4a;
}

.badge--member {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.hero__name {
  font-family: var(--font-serif);
  font-size: clamp(24px, 3.5vw, 32px);
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 8px;
}

.hero__meta {
  font-size: 13.5px;
  color: var(--ink-soft);
}

.hero__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 20px;
}

/* ---------- 按钮 ---------- */
.btn {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
  color: var(--ink-soft);
  font-size: 13.5px;
  padding: 9px 18px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
  white-space: nowrap;
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

.btn--danger-solid {
  background: var(--danger-soft);
  color: var(--danger);
  border-color: rgba(217, 119, 106, 0.4);
}

.btn--danger-solid:hover {
  background: var(--danger);
  color: #fff;
}

.btn--ghost {
  background: rgba(255, 255, 255, 0.7);
}

.btn--sm {
  padding: 6px 14px;
  font-size: 12.5px;
}

/* ---------- 申请列表 ---------- */
.app-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.app-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid var(--line);
  border-radius: 14px;
}

.app-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.app-item__time {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-faint);
}

.app-item__actions {
  display: flex;
  gap: 8px;
}

/* ---------- 成员 ---------- */
.member-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.member {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid var(--line);
  border-radius: 14px;
}

.member__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent-deep);
  display: grid;
  place-items: center;
  font-weight: 700;
  flex-shrink: 0;
}

.member__name {
  font-size: 14px;
  font-weight: 550;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member__ops {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.linklike {
  border: none;
  background: none;
  color: var(--accent-deep);
  font-size: 12.5px;
  cursor: pointer;
}

.linklike--danger {
  color: var(--danger);
}

/* ---------- 任务列表 ---------- */
.task-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  padding: 16px 18px;
  transition: transform 0.18s, box-shadow 0.18s;
}

.task-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-sm);
}

.task-card.type-exercise { border-top: 3px solid #e8895a; }
.task-card.type-work { border-top: 3px solid #5b8def; }
.task-card.type-study { border-top: 3px solid #8b7af0; }
.task-card.type-life { border-top: 3px solid #3bb88c; }
.task-card.type-other { border-top: 3px solid #8a94a0; }

.task-card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.tag--all {
  background: #eef2f6;
  color: #64748b;
}

.tag--assigned {
  background: #f6ecdb;
  color: #bd8a4a;
}

.tag--done {
  background: var(--ok-soft);
  color: var(--ok);
}

.task-type-tag {
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.tt-exercise { background: #fbe9dc; color: #e8895a; }
.tt-work { background: #e6eefd; color: #5b8def; }
.tt-study { background: #ece6fc; color: #8b7af0; }
.tt-life { background: #e1f5ec; color: #3bb88c; }
.tt-other { background: #eff2f5; color: #8a94a0; }

.task-card__name {
  font-size: 15.5px;
  font-weight: 600;
  color: var(--ink);
}

.task-card__content {
  font-size: 13px;
  color: var(--ink-soft);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.task-card__meta {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--ink-faint);
}

.dot {
  color: var(--ink-faint);
}

.empty {
  color: var(--ink-faint);
  font-size: 14px;
  text-align: center;
  padding: 26px 0;
}

/* ---------- 弹窗表单 ---------- */
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
  line-height: 1.6;
  resize: none;
}

.w-full {
  width: 100%;
}

.dialog-tip {
  font-size: 13.5px;
  color: var(--ink-soft);
  margin-bottom: 14px;
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

@media (max-width: 640px) {
  .member-grid,
  .task-list {
    grid-template-columns: 1fr;
  }
}
</style>