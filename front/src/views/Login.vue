<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>登录11111</h2>
      <p class="subtitle">欢迎回来，请登录你的账号</p>

      <div v-if="error" class="msg error">{{ error }}</div>

      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>用户名</label>
          <input v-model.trim="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="form-item">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <p class="switch-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function onSubmit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    store.setAuth(res.data.token, res.data.user)
    router.push('/home')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
