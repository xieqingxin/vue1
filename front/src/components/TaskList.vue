<template>
  <div>
    <ul v-if="tasks.length" class="task-list">
      <li
        v-for="t in tasks"
        :key="t.id"
        class="task-card"
        :class="['type-' + (t.type || 'other'), { done: t.status === 1, expired: t.status === 2 }]"
        @click="router.push(`/task/${t.id}`)"
      >
        <div class="task-card__head">
          <span class="task-status" :class="statusClass(t.status)">{{ statusLabel(t.status) }}</span>
          <span class="task-type-tag" :class="'tag-' + (t.type || 'other')">
            <CategoryIcon :type="t.type || 'other'" />
            {{ typeLabel(t.type) }}
          </span>
          <span class="task-card__reward">奖励 {{ t.reward }}</span>
        </div>

        <h3 class="task-card__name">{{ t.name }}</h3>

        <p v-if="t.content" class="task-card__content">{{ t.content }}</p>

        <div class="task-card__meta">
          <span>起 {{ formatTime(t.startTime) }}</span>
          <span class="dot">·</span>
          <span>止 {{ formatTime(t.endTime) }}</span>
          <template v-if="t.status === 1 && t.completedAt">
            <span class="dot">·</span>
            <span class="meta-done">完成于 {{ formatTime(t.completedAt) }}</span>
          </template>
        </div>

        <!-- 操作区阻止冒泡，避免触发整卡跳转 -->
        <div class="task-card__actions" @click.stop>
          <button v-if="t.status === 0" class="act act--done" @click="askComplete(t)">标记完成</button>
          <button class="act act--danger" @click="askDelete(t)">删除</button>
        </div>
      </li>
    </ul>
    <p v-else class="empty">{{ emptyText }}</p>

    <!-- 完成确认弹窗（Teleport 到 body，保证在最上层） -->
    <Teleport to="body">
      <div v-if="confirmTask" class="modal-mask" @click.self="closeConfirm">
        <div class="modal" role="dialog" aria-modal="true">
          <h3 class="modal__title">确认完成任务</h3>
          <p class="modal__text">
            任务「<strong>{{ confirmTask.name }}</strong>」确认任务是否完成？
          </p>
          <div class="modal__actions">
            <button class="act" @click="closeConfirm">取消</button>
            <button class="act act--done" :disabled="completing" @click="confirmComplete">
              {{ completing ? '提交中...' : '确认完成' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 删除确认弹窗（与完成确认同款样式） -->
      <div v-if="pendingDelete" class="modal-mask" @click.self="closeDelete">
        <div class="modal" role="dialog" aria-modal="true">
          <h3 class="modal__title">确认删除任务</h3>
          <p class="modal__text">
            任务「<strong>{{ pendingDelete.name }}</strong>」删除后不可恢复，确认删除吗？
          </p>
          <div class="modal__actions">
            <button class="act" @click="closeDelete">取消</button>
            <button class="act act--danger-solid" :disabled="deleting" @click="confirmDelete">
              {{ deleting ? '删除中...' : '确认删除' }}
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
    </Teleport>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { updateTaskStatus, deleteTask } from '../api/user'
import CategoryIcon from './CategoryIcon.vue'

const props = defineProps({
  // 要展示的任务列表（由父组件负责筛选）
  tasks: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无任务' }
})
const emit = defineEmits(['refresh', 'error'])

const router = useRouter()

const confirmTask = ref(null)
const completing = ref(false)
const praiseText = ref('')
const pendingDelete = ref(null)
const deleting = ref(false)

// 任务类型定义（与首页保持一致）
const taskTypes = [
  { label: '锻炼', value: 'exercise' },
  { label: '工作', value: 'work' },
  { label: '学习', value: 'study' },
  { label: '生活', value: 'life' },
  { label: '其他', value: 'other' }
]

function typeLabel(v) {
  const tp = taskTypes.find((t) => t.value === (v || 'other'))
  return tp ? tp.label : '其他'
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

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
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
  try {
    await updateTaskStatus(t.id, 1)
    confirmTask.value = null
    emit('refresh')
    praiseText.value = PRAISES[Math.floor(Math.random() * PRAISES.length)]
  } catch (e) {
    emit('error', e.message)
  } finally {
    completing.value = false
  }
}

function closePraise() {
  praiseText.value = ''
}

function askDelete(t) {
  pendingDelete.value = t
}

function closeDelete() {
  if (deleting.value) return
  pendingDelete.value = null
}

async function confirmDelete() {
  const t = pendingDelete.value
  if (!t || deleting.value) return
  deleting.value = true
  try {
    await deleteTask(t.id)
    pendingDelete.value = null
    emit('refresh')
  } catch (e) {
    emit('error', e.message)
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.task-list {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

/* 任务卡片：现代扁平化，大白卡 + 极浅阴影 + 大圆角 */
.task-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  background: var(--surface);
  backdrop-filter: blur(18px) saturate(1.2);
  -webkit-backdrop-filter: blur(18px) saturate(1.2);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius);
  padding: 20px 22px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.2s;
}

.task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 22px 44px -24px rgba(96, 128, 118, 0.4);
}

/* 每个分类顶部一条淡淡的主题色描边 */
.task-card.type-exercise { border-top: 3px solid var(--c-exercise, #e8895a); }
.task-card.type-work { border-top: 3px solid var(--c-work, #5b8def); }
.task-card.type-study { border-top: 3px solid var(--c-study, #8b7af0); }
.task-card.type-life { border-top: 3px solid var(--c-life, #3bb88c); }
.task-card.type-other { border-top: 3px solid var(--c-other, #8a94a0); }

/* 分类主题色（供标签与图标着色，与首页一致） */
.task-card.type-exercise { --tone: #e8895a; --tone-soft: #fbe9dc; }
.task-card.type-work { --tone: #5b8def; --tone-soft: #e6eefd; }
.task-card.type-study { --tone: #8b7af0; --tone-soft: #ece6fc; }
.task-card.type-life { --tone: #3bb88c; --tone-soft: #e1f5ec; }
.task-card.type-other { --tone: #8a94a0; --tone-soft: #eff2f5; }

.task-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 999px;
  flex-shrink: 0;
}

.task-status.is-doing {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.task-status.is-done {
  background: var(--ok-soft);
  color: var(--ok);
}

.task-status.is-expired {
  background: var(--danger-soft);
  color: var(--danger);
}

.task-type-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--tone-soft, #eef2f5);
  color: var(--tone, #64748b);
  flex-shrink: 0;
}

.task-type-tag :deep(svg) {
  width: 14px;
  height: 14px;
}

.task-card__reward {
  margin-left: auto;
  font-size: 13px;
  font-weight: 700;
  color: #bd8a4a;
  background: #f6ecdb;
  padding: 4px 12px;
  border-radius: 999px;
  flex-shrink: 0;
}

.task-card__name {
  font-size: 16.5px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.task-card.done .task-card__name {
  text-decoration: line-through;
  color: var(--ink-faint);
}

.task-card.expired .task-card__name {
  color: var(--ink-faint);
}

.task-card__content {
  font-size: 13.5px;
  color: var(--ink-soft);
  line-height: 1.65;
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

.task-card__meta .dot {
  color: rgba(120, 150, 140, 0.35);
}

.task-card__meta .meta-done {
  color: var(--accent-deep);
  font-weight: 600;
}

.task-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.act {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
  color: var(--ink-soft);
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
  white-space: nowrap;
}

.act:hover {
  border-color: var(--accent);
  color: var(--accent-deep);
  background: var(--accent-soft);
}

.act--done {
  border-color: var(--accent);
  color: var(--accent-deep);
  background: var(--accent-soft);
}

.act--done:hover {
  background: var(--accent-deep);
  border-color: var(--accent-deep);
  color: #fff;
}

.act--danger {
  color: var(--danger);
  border-color: rgba(217, 119, 106, 0.22);
}

.act--danger:hover {
  border-color: rgba(217, 119, 106, 0.4);
  color: var(--danger);
  background: var(--danger-soft);
}

.act--danger-solid {
  border-color: rgba(217, 119, 106, 0.4);
  color: var(--danger);
  background: var(--danger-soft);
}

.act--danger-solid:hover {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}

.act:disabled {
  opacity: 0.6;
  cursor: default;
}

.empty {
  color: var(--ink-faint);
  font-size: 14px;
  text-align: center;
  padding: 44px 0;
  background: var(--surface);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  border: 1px dashed var(--line);
  border-radius: var(--radius);
}

/* ---------- 弹窗 ---------- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(96, 128, 118, 0.32);
  backdrop-filter: blur(3px);
  display: grid;
  place-items: center;
  padding: 20px;
  animation: fadeIn 0.2s ease both;
}

.modal {
  width: min(400px, 100%);
  background: var(--surface-solid);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: 0 30px 60px -30px rgba(96, 128, 118, 0.45);
  padding: 26px;
  animation: pop 0.28s cubic-bezier(0.22, 1, 0.36, 1) both;
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

@media (max-width: 680px) {
  .task-list {
    grid-template-columns: 1fr;
  }
}
</style>