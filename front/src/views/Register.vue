<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册</h2>
      <p class="subtitle">创建一个新账号</p>

      <div v-if="error" class="msg error">{{ error }}</div>
      <div v-if="success" class="msg success">{{ success }}</div>

      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>用户名 <span style="color:#9ca3af">(3-20 位)</span></label>
          <input v-model.trim="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="form-item">
          <label>昵称</label>
          <input v-model.trim="form.nickname" type="text" placeholder="选填，默认与用户名相同" />
        </div>
        <div class="form-item">
          <label>邮箱</label>
          <input v-model.trim="form.email" type="email" placeholder="选填" />
        </div>
        <div class="form-item">
          <label>密码 <span style="color:#9ca3af">(6-32 位)</span></label>
          <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="new-password" />
        </div>
        <div class="form-item">
          <label>确认密码</label>
          <input v-model="form.confirm" type="password" placeholder="请再次输入密码" autocomplete="new-password" />
        </div>
        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </form>

      <p class="switch-link">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'

const router = useRouter()

const form = reactive({ username: '', nickname: '', email: '', password: '', confirm: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')

async function onSubmit() {
  error.value = ''
  success.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  if (form.password !== form.confirm) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email
    })
    success.value = '注册成功，即将跳转到登录页...'
    setTimeout(() => router.push('/login'), 1200)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
