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
        <div class="task-card__main">
          <div class="task-card__head">
            <span class="task-status" :class="statusClass(t.status)">{{ statusLabel(t.status) }}</span>
            <span class="task-type-tag" :class="'tag-' + (t.type || 'other')">
              {{ typeLabel(t.type) }}
            </span>
            <h3 class="task-card__name">{{ t.name }}</h3>
            <span class="task-card__reward">奖励 {{ t.reward }}</span>
          </div>
          <p v-if="t.content" class="task-card__content">{{ t.content }}</p>
          <div class="task-card__meta">
            <span>开始 {{ formatTime(t.startTime) }}</span>
            <span class="dot">·</span>
            <span>截止 {{ formatTime(t.endTime) }}</span>
            <template v-if="t.status === 1 && t.completedAt">
              <span class="dot">·</span>
              <span class="meta-done">完成于 {{ formatTime(t.completedAt) }}</span>
            </template>
          </div>
        </div>
        <!-- 操作区阻止冒泡，避免触发整卡跳转 -->
        <div class="task-card__actions" @click.stop>
          <button v-if="t.status === 0" class="act act--done" @click="askComplete(t)">标记完成</button>
          <button class="act act--danger" @click="onDelete(t)">删除</button>
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
    </Teleport>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { updateTaskStatus, deleteTask } from '../api/user'

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

// 任务类型定义（与首页保持一致）
const taskTypes = [
  { label: '锻炼', value: 'exercise', icon: '🏃' },
  { label: '工作', value: 'work', icon: '💼' },
  { label: '学习', value: 'study', icon: '📚' },
  { label: '生活', value: 'life', icon: '🏠' },
  { label: '其他', value: 'other', icon: '📌' }
]

function typeLabel(v) {
  const tp = taskTypes.find((t) => t.value === (v || 'other'))
  return tp ? `${tp.icon} ${tp.label}` : '📌 其他'
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

async function onDelete(t) {
  if (!window.confirm(`确定删除任务「${t.name}」吗？`)) return
  try {
    await deleteTask(t.id)
    emit('refresh')
  } catch (e) {
    emit('error', e.message)
  }
}
</script>

<style scoped>
.task-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 任务卡片：贴纸样式（彩色描边 + 硬阴影 + 轻微倾斜） */
.task-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  cursor: pointer;
  background: var(--surface);
  border: 2px solid var(--tc, var(--accent));
  border-radius: 16px;
  padding: 16px 18px;
  box-shadow: 3px 4px 0 rgba(23, 50, 44, 0.1);
  transform: rotate(-0.45deg);
  transition: box-shadow 0.18s, transform 0.18s;
}

.task-list li:nth-child(even) .task-card {
  transform: rotate(0.5deg);
}

.task-card:hover {
  transform: rotate(0deg) translateY(-2px);
  box-shadow: 3px 5px 0 rgba(23, 50, 44, 0.14), var(--shadow-md);
}

/* 贴纸角落的小胶带 */
.task-card::before {
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

.task-card.done .task-card__name {
  text-decoration: line-through;
  color: var(--ink-faint);
}

/* 不同任务类型的贴纸颜色 */
.task-card.type-exercise { --tc: #e2703a; }
.task-card.type-work { --tc: #2563eb; }
.task-card.type-life { --tc: #0d9488; }
.task-card.type-other { --tc: #64748b; }

/* 学习贴纸：纸张背景 */
.task-card.type-study {
  --tc: #7c3aed;
  border-color: #e6dfc8;
  border-radius: 6px;
  background:
    linear-gradient(90deg, transparent 0 14px, rgba(224, 138, 138, 0.5) 14px 15px, transparent 15px),
    repeating-linear-gradient(transparent 0 27px, rgba(124, 58, 237, 0.12) 27px 28px),
    #fffdf5;
}

.task-card.done {
  --tc: #94a3b8;
  opacity: 0.75;
  background: var(--surface);
}

.task-card.expired {
  --tc: #dc2626;
  background: linear-gradient(90deg, rgba(220, 38, 38, 0.05), var(--surface) 42%);
}

.task-card.expired .task-card__name {
  color: var(--ink-faint);
}

/* 任务类型标签 */
.task-type-tag {
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 6px;
  flex-shrink: 0;
}

.task-type-tag.tag-exercise {
  background: rgba(226, 112, 58, 0.12);
  color: #c2551d;
}

.task-type-tag.tag-work {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.task-type-tag.tag-study {
  background: rgba(124, 58, 237, 0.1);
  color: #6d28d9;
}

.task-type-tag.tag-life {
  background: rgba(13, 148, 136, 0.1);
  color: #0f766e;
}

.task-type-tag.tag-other {
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
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

.task-status.is-expired {
  background: #fee2e2;
  color: #dc2626;
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

.task-card__meta .meta-done {
  color: var(--accent-deep);
  font-weight: 600;
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

/* ---------- 弹窗 ---------- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(23, 50, 44, 0.45);
  backdrop-filter: blur(3px);
  display: grid;
  place-items: center;
  padding: 20px;
  animation: fadeIn 0.2s ease both;
}

.modal {
  width: min(400px, 100%);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: 24px;
  animation: pop 0.28s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.modal__title {
  font-size: 17px;
  font-weight: 650;
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

@media (max-width: 640px) {
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
