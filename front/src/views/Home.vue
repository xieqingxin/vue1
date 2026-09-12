<template>
  <div class="home">
    <header class="topbar">
      <div class="brand">
        <span class="brand__dot">见</span>
        <span>用户中心</span>
      </div>
      <div class="user-info">
        <span class="avatar" aria-hidden="true">
          <img v-if="user && user.avatar" :src="user.avatar" alt="avatar" />
          <template v-else>{{ initial }}</template>
        </span>
        <span class="name">{{ user ? user.nickname || user.username : '' }}</span>
        <button class="link-btn" @click="onLogout">退出登录</button>
      </div>
    </header>

    <main class="container">
      <div class="greeting stagger">
        <h1>{{ greeting }}，{{ user ? user.nickname || user.username : '' }}</h1>
        <p>在这里管理你的资料与文件，文件内容保存在数据库中。</p>
      </div>

      <div class="layout">
        <aside class="profile">
          <h2 class="section-title">个人信息</h2>
          <ul class="info-list" v-if="user">
            <li><span>ID</span><b>{{ user.id }}</b></li>
            <li><span>用户名</span><b>{{ user.username }}</b></li>
            <li><span>昵称</span><b>{{ user.nickname || '-' }}</b></li>
            <li><span>邮箱</span><b>{{ user.email || '-' }}</b></li>
            <li><span>注册时间</span><b>{{ user.createdAt || '-' }}</b></li>
          </ul>
        </aside>

        <section class="files">
          <div class="files__head">
            <h2 class="section-title">我的文件</h2>
            <span class="files__count" v-if="files.length">{{ files.length }} 个文件</span>
          </div>

          <div v-if="error" class="msg error">{{ error }}</div>
          <div v-if="tip" class="msg success">{{ tip }}</div>

          <div class="upload-row">
            <input type="file" ref="fileInput" class="file-input" />
            <button class="btn-sm" :disabled="uploading" @click="onUpload">
              {{ uploading ? '上传中...' : '上传文件' }}
            </button>
          </div>

          <table class="file-table" v-if="files.length">
            <thead>
              <tr>
                <th>文件名</th>
                <th>类型</th>
                <th>大小</th>
                <th>上传时间</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in files" :key="f.id">
                <td class="cell-name">{{ f.name }}</td>
                <td>{{ f.contentType || '-' }}</td>
                <td>{{ formatSize(f.size) }}</td>
                <td>{{ f.createdAt }}</td>
                <td><a class="dl" :href="downloadUrl(f.id)" target="_blank">下载</a></td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty">暂无文件，选择文件后点击「上传文件」。</p>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, uploadFile, listFiles } from '../api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()

const user = ref(store.user)
const files = ref([])
const uploading = ref(false)
const error = ref('')
const tip = ref('')
const fileInput = ref(null)

const API_BASE = import.meta.env.DEV ? '/api' : ''

const initial = computed(() => {
  const name = user.value ? user.value.nickname || user.value.username : ''
  return name ? name.charAt(0).toUpperCase() : '?'
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

function downloadUrl(id) {
  return `${API_BASE}/files/download/${id}`
}

function formatSize(size) {
  if (size == null) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

async function loadProfile() {
  try {
    const res = await getProfile()
    user.value = res.data
    store.setUser(res.data)
  } catch (e) {
    error.value = e.message
  }
}

async function loadFiles() {
  try {
    const res = await listFiles()
    files.value = res.data || []
  } catch (e) {
    error.value = e.message
  }
}

async function onUpload() {
  error.value = ''
  tip.value = ''
  const input = fileInput.value
  if (!input || !input.files || !input.files[0]) {
    error.value = '请先选择文件'
    return
  }
  uploading.value = true
  try {
    await uploadFile(input.files[0])
    tip.value = '上传成功'
    input.value = ''
    await loadFiles()
  } catch (e) {
    error.value = e.message
  } finally {
    uploading.value = false
  }
}

function onLogout() {
  store.logout()
  router.push('/login')
}

onMounted(() => {
  loadProfile()
  loadFiles()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  min-height: 100svh;
  background:
    radial-gradient(720px 320px at 85% -10%, rgba(22, 160, 133, 0.1), transparent 65%),
    radial-gradient(560px 280px at -5% 0%, rgba(125, 211, 252, 0.14), transparent 60%),
    var(--canvas);
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

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0.01em;
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
  font-size: 13px;
  font-weight: 700;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent-soft);
  color: var(--accent-deep);
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 650;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  font-size: 14px;
  color: var(--ink);
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
  max-width: 960px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 44px) clamp(16px, 4vw, 24px) 64px;
}

.greeting {
  margin-bottom: 28px;
}

.greeting h1 {
  font-size: clamp(24px, 3.2vw, 32px);
  font-weight: 650;
  letter-spacing: -0.015em;
  margin-bottom: 8px;
}

.greeting p {
  font-size: 14.5px;
  color: var(--ink-soft);
}

.layout {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: clamp(20px, 3vw, 36px);
  align-items: start;
}

.section-title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
  margin-bottom: 14px;
}

/* ---------- 个人信息：纯排版，不用卡片 ---------- */
.profile {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.1s both;
}

.info-list {
  list-style: none;
  border-top: 1px solid var(--line);
}

.info-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 2px;
  border-bottom: 1px solid var(--line);
  font-size: 13.5px;
}

.info-list li span {
  color: var(--ink-faint);
  flex-shrink: 0;
}

.info-list li b {
  font-weight: 550;
  color: var(--ink);
  text-align: right;
  word-break: break-all;
}

/* ---------- 文件区：唯一的交互容器 ---------- */
.files {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  padding: clamp(18px, 2.6vw, 28px);
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.18s both;
}

.files__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.files__count {
  font-size: 12.5px;
  color: var(--ink-faint);
}

.upload-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 4px 0 18px;
}

.file-input {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--ink-soft);
  padding: 10px 12px;
  border: 1px dashed #c9ddd6;
  border-radius: 12px;
  background: var(--canvas);
  transition: border-color 0.18s, background 0.18s;
}

.file-input:hover {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.file-input::file-selector-button {
  font-family: inherit;
  font-size: 12.5px;
  margin-right: 10px;
  padding: 6px 10px;
  border: none;
  border-radius: 8px;
  background: var(--accent-soft);
  color: var(--accent-deep);
  cursor: pointer;
}

.btn-sm {
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 6px 16px -8px rgba(22, 160, 133, 0.7);
  transition: background 0.18s, transform 0.18s;
  flex-shrink: 0;
}

.btn-sm:hover:not(:disabled) {
  background: var(--accent-deep);
  transform: translateY(-1px);
}

.btn-sm:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.file-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

.file-table th,
.file-table td {
  text-align: left;
  padding: 11px 10px;
  border-bottom: 1px solid var(--line);
}

.file-table th {
  color: var(--ink-faint);
  font-weight: 550;
  font-size: 12.5px;
}

.file-table tbody tr {
  transition: background 0.15s;
}

.file-table tbody tr:hover {
  background: var(--accent-soft);
}

.file-table tbody tr:last-child td {
  border-bottom: none;
}

.cell-name {
  font-weight: 550;
  color: var(--ink);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dl {
  font-weight: 600;
  font-size: 13px;
  color: var(--accent-deep);
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid #bfe6dc;
  transition: background 0.18s, color 0.18s;
}

.dl:hover {
  background: var(--accent);
  color: #fff;
}

.empty {
  color: var(--ink-faint);
  font-size: 14px;
  text-align: center;
  padding: 28px 0;
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

.msg.success {
  background: var(--ok-soft);
  color: var(--ok);
  border: 1px solid #cdeed8;
}

.stagger > * {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@media (max-width: 720px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .profile {
    order: 2;
  }

  .files {
    order: 1;
  }

  .upload-row {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-sm {
    width: 100%;
  }
}
</style>
