<template>
  <div class="home">
    <header class="topbar">
      <div class="brand">用户中心</div>
      <div class="user-info">
        <img v-if="user && user.avatar" :src="user.avatar" class="avatar" alt="avatar" />
        <span class="name">{{ user ? user.nickname || user.username : '' }}</span>
        <button class="link-btn" @click="onLogout">退出登录</button>
      </div>
    </header>

    <main class="container">
      <section class="card">
        <h3>个人信息</h3>
        <ul class="info-list" v-if="user">
          <li><span>ID</span><b>{{ user.id }}</b></li>
          <li><span>用户名</span><b>{{ user.username }}</b></li>
          <li><span>昵称</span><b>{{ user.nickname || '-' }}</b></li>
          <li><span>邮箱</span><b>{{ user.email || '-' }}</b></li>
          <li><span>注册时间</span><b>{{ user.createdAt || '-' }}</b></li>
        </ul>
      </section>

      <section class="card">
        <h3>文件存储（内容保存到数据库）</h3>
        <div v-if="error" class="msg error">{{ error }}</div>
        <div v-if="tip" class="msg success">{{ tip }}</div>

        <div class="upload-row">
          <input type="file" ref="fileInput" />
          <button class="btn-sm" :disabled="uploading" @click="onUpload">
            {{ uploading ? '上传中...' : '上传文件' }}
          </button>
        </div>

        <table class="file-table" v-if="files.length">
          <thead>
            <tr>
              <th>ID</th>
              <th>文件名</th>
              <th>类型</th>
              <th>大小</th>
              <th>上传时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in files" :key="f.id">
              <td>{{ f.id }}</td>
              <td>{{ f.name }}</td>
              <td>{{ f.contentType || '-' }}</td>
              <td>{{ formatSize(f.size) }}</td>
              <td>{{ f.createdAt }}</td>
              <td><a :href="downloadUrl(f.id)" target="_blank">下载</a></td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty">暂无文件，请先上传。</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
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
}

.topbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
}

.brand {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
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
  object-fit: cover;
  background: #eee;
}

.name {
  font-size: 14px;
}

.link-btn {
  border: none;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-size: 14px;
}

.container {
  max-width: 860px;
  margin: 24px auto;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.card h3 {
  margin-bottom: 16px;
  font-size: 16px;
}

.info-list {
  list-style: none;
}

.info-list li {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
  font-size: 14px;
}

.info-list li span {
  width: 100px;
  color: #6b7280;
}

.upload-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.btn-sm {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: #667eea;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.file-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.file-table th,
.file-table td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid #f0f0f0;
}

.file-table th {
  color: #6b7280;
  font-weight: 500;
}

.empty {
  color: #9ca3af;
  font-size: 14px;
}

.msg {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 12px;
}

.msg.error {
  background: #fef2f2;
  color: #b91c1c;
}

.msg.success {
  background: #f0fdf4;
  color: #15803d;
}
</style>
