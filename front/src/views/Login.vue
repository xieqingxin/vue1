<template>
  <div class="auth-page">
    <img
      class="auth-bg"
      :src="bgImg"
      alt=""
    />
    <div class="auth-brand">
      <span class="auth-brand__dot" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
      </span>
      <span>自律计划任务平台</span>
    </div>

    <div class="auth-card stagger">
      <h2>登录</h2>
      <p class="subtitle">欢迎回来，请登录你的账号</p>

      <div v-if="error" class="msg error">{{ error }}</div>

      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label for="login-username">用户名</label>
          <input id="login-username" v-model.trim="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="form-item">
          <label for="login-password">密码</label>
          <input id="login-password" v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
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
import bgImg from '../assets/bg/login.jpg' // 登录页背景：晨雾森林实景（本地资源）

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
