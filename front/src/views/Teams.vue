<template>
  <div class="teams-page">
    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goHome">← 返回首页</button>
        <div class="brand">
          <span class="brand__dot" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </span>
          <span>团队</span>
        </div>
      </div>
    </header>

    <main class="container">
      <!-- 创建团队 -->
      <section class="card create-card">
        <h2 class="section-title">创建团队</h2>
        <div v-if="createError" class="msg error">{{ createError }}</div>
        <div class="create-form">
          <input v-model.trim="form.name" class="input" placeholder="团队名称" maxlength="64" />
          <select v-model="form.category" class="input input--select">
            <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
          </select>
          <div class="size-row">
            <button
              v-for="s in sizeOptions"
              :key="s.value"
              class="chip"
              :class="{ active: sizeMode === s.value }"
              @click="sizeMode = s.value"
            >
              {{ s.label }}
            </button>
            <input
              v-if="sizeMode === 'custom'"
              v-model.number="form.maxSize"
              type="number"
              min="1"
              class="input input--num"
              placeholder="自定义人数"
            />
          </div>
          <button class="btn-primary" :disabled="creating" @click="onCreate">
            {{ creating ? '创建中...' : '创建团队' }}
          </button>
        </div>
      </section>

      <!-- 我加入的团队 -->
      <section class="card">
        <h2 class="section-title">我加入的团队</h2>
        <div v-if="mineError" class="msg error">{{ mineError }}</div>
        <div v-if="myTeams.length" class="team-grid">
          <button
            v-for="t in myTeams"
            :key="t.id"
            class="team-card"
            @click="router.push(`/teams/${t.id}`)"
          >
            <div class="team-card__head">
              <span class="team-card__name">{{ t.name }}</span>
              <span class="tag" :class="t.myRole === 'leader' ? 'tag--leader' : 'tag--member'">
                {{ t.myRole === 'leader' ? '队长' : '成员' }}
              </span>
            </div>
            <div class="team-card__meta">
              <span class="cat-dot" :style="{ background: catColor(t.category) }"></span>
              <span>{{ catLabel(t.category) }}</span>
              <span class="dot">·</span>
              <span>{{ t.memberCount }}/{{ t.maxSize }} 人</span>
            </div>
            <div class="team-card__leader">队长 {{ t.leaderNickname || '-' }}</div>
          </button>
        </div>
        <p v-else class="empty">还没有加入任何团队，去下方发现团队申请加入吧。</p>
      </section>

      <!-- 发现团队 -->
      <section class="card">
        <h2 class="section-title">发现团队</h2>
        <div class="discover-bar">
          <input v-model.trim="keyword" class="input input--search" placeholder="搜索团队名称" @input="onSearch" />
          <div class="cat-filters">
            <button
              v-for="c in [{ label: '全部', value: '' }, ...categories]"
              :key="'f' + c.value"
              class="chip"
              :class="{ active: filterCategory === c.value }"
              @click="setCategory(c.value)"
            >
              {{ c.label }}
            </button>
          </div>
        </div>
        <div v-if="discoverError" class="msg error">{{ discoverError }}</div>
        <div v-if="discoverList.length" class="team-grid">
          <div v-for="t in discoverList" :key="t.id" class="team-card">
            <div class="team-card__head">
              <span class="team-card__name">{{ t.name }}</span>
              <span v-if="t.myRole" class="tag" :class="t.myRole === 'leader' ? 'tag--leader' : 'tag--member'">
                {{ t.myRole === 'leader' ? '队长' : '成员' }}
              </span>
            </div>
            <div class="team-card__meta">
              <span class="cat-dot" :style="{ background: catColor(t.category) }"></span>
              <span>{{ catLabel(t.category) }}</span>
              <span class="dot">·</span>
              <span>{{ t.memberCount }}/{{ t.maxSize }} 人</span>
            </div>
            <div class="team-card__leader">队长 {{ t.leaderNickname || '-' }}</div>
            <div class="team-card__actions">
              <button v-if="t.myRole" class="btn-primary btn-primary--sm" @click="router.push(`/teams/${t.id}`)">进入</button>
              <button
                v-else-if="t.myStatus === 'pending'"
                class="btn-ghost btn-ghost--sm"
                disabled
              >待审核</button>
              <button v-else class="btn-primary btn-primary--sm" @click="onApply(t)">申请加入</button>
            </div>
          </div>
        </div>
        <p v-else class="empty">没有找到相关团队。</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTeam, listMyTeams, recommendedTeams, searchTeams, applyJoin } from '../api/team'

const router = useRouter()

const categories = [
  { label: '锻炼', value: 'exercise' },
  { label: '工作', value: 'work' },
  { label: '学习', value: 'study' },
  { label: '生活', value: 'life' },
  { label: '其他', value: 'other' }
]

const sizeOptions = [
  { label: '30 人', value: 30 },
  { label: '50 人', value: 50 },
  { label: '100 人', value: 100 },
  { label: '自定义', value: 'custom' }
]

const form = ref({ name: '', category: 'other', maxSize: 30 })
const sizeMode = ref(30)
const creating = ref(false)
const createError = ref('')

const myTeams = ref([])
const mineError = ref('')

const keyword = ref('')
const filterCategory = ref('')
const discoverList = ref([])
const discoverError = ref('')

function catLabel(v) {
  return (categories.find((c) => c.value === v) || categories[4]).label
}

function catColor(v) {
  const map = { exercise: '#e8895a', work: '#5b8def', study: '#8b7af0', life: '#3bb88c', other: '#8a94a0' }
  return map[v] || map.other
}

function goHome() {
  router.push('/home')
}

async function onCreate() {
  createError.value = ''
  if (!form.value.name) {
    createError.value = '请填写团队名称'
    return
  }
  const maxSize = sizeMode.value === 'custom' ? form.value.maxSize : sizeMode.value
  if (!maxSize || maxSize < 1) {
    createError.value = '请填写自定义人数'
    return
  }
  creating.value = true
  try {
    await createTeam({ name: form.value.name, category: form.value.category, maxSize })
    ElMessage.success('团队创建成功')
    form.value.name = ''
    await Promise.all([loadMine(), loadDiscover()])
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

async function loadMine() {
  mineError.value = ''
  try {
    const res = await listMyTeams()
    myTeams.value = res.data || []
  } catch (e) {
    mineError.value = e.message
  }
}

async function loadDiscover() {
  discoverError.value = ''
  try {
    if (keyword.value || filterCategory.value) {
      const res = await searchTeams({ keyword: keyword.value || undefined, category: filterCategory.value || undefined })
      discoverList.value = res.data || []
    } else {
      const res = await recommendedTeams()
      discoverList.value = res.data || []
    }
  } catch (e) {
    discoverError.value = e.message
  }
}

function onSearch() {
  loadDiscover()
}

function setCategory(v) {
  filterCategory.value = v
  loadDiscover()
}

async function onApply(t) {
  try {
    await applyJoin(t.id)
    ElMessage.success('申请已提交，等待队长审核')
    await loadDiscover()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(() => {
  loadMine()
  loadDiscover()
})
</script>

<style scoped>
.teams-page {
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
  max-width: 840px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 40px) clamp(16px, 4vw, 24px) 72px;
  display: flex;
  flex-direction: column;
  gap: 26px;
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
  font-size: 15px;
  font-weight: 650;
  color: var(--ink);
  padding-left: 12px;
  border-left: 4px solid var(--accent);
  line-height: 1;
  margin-bottom: 18px;
}

/* ---------- 表单 ---------- */
.create-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input {
  height: 46px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-family: inherit;
  color: var(--ink);
  outline: none;
  transition: border-color 0.18s, box-shadow 0.18s;
}

.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-soft);
  background: #ffffff;
}

.input--select {
  width: 100%;
}

.input--num {
  width: 140px;
}

.size-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
  color: var(--ink-soft);
  font-size: 13px;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.18s, background 0.18s, border-color 0.18s;
}

.chip:hover {
  color: var(--ink);
}

.chip.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-weight: 600;
}

.btn-primary {
  height: 46px;
  border: 1px solid rgba(121, 183, 166, 0.5);
  border-radius: 999px;
  background: rgba(121, 183, 166, 0.9);
  color: #fff;
  font-size: 14.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s, transform 0.18s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--accent);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-primary--sm {
  height: 36px;
  padding: 0 20px;
  font-size: 13px;
}

/* ---------- 团队网格 ---------- */
.team-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.team-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  padding: 18px;
  transition: transform 0.18s, box-shadow 0.18s;
}

.team-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-sm);
}

.team-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.team-card__name {
  font-size: 16px;
  font-weight: 650;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}

.tag--leader {
  background: #f6ecdb;
  color: #bd8a4a;
}

.tag--member {
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.team-card__meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--ink-soft);
}

.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot {
  color: var(--ink-faint);
}

.team-card__leader {
  font-size: 12.5px;
  color: var(--ink-faint);
}

.team-card__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 2px;
}

.discover-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 18px;
}

.cat-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.input--search {
  width: 100%;
}

.msg {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 13.5px;
  margin-bottom: 16px;
}

.msg.error {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid rgba(217, 119, 106, 0.22);
}

.empty {
  color: var(--ink-faint);
  font-size: 14px;
  text-align: center;
  padding: 30px 0;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

@media (max-width: 640px) {
  .team-grid {
    grid-template-columns: 1fr;
  }
}
</style>