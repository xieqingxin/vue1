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

    <div class="auth-card auth-card--compact stagger">
      <h2>注册</h2>
      <p class="subtitle">创建一个新账号</p>

      <div v-if="error" class="msg error">{{ error }}</div>
      <div v-if="success" class="msg success">{{ success }}</div>

      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label for="reg-username">用户名 <span class="hint">(3-20 位)</span></label>
          <input id="reg-username" v-model.trim="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label for="reg-nickname">昵称</label>
            <input id="reg-nickname" v-model.trim="form.nickname" type="text" placeholder="选填" />
          </div>
          <div class="form-item">
            <label for="reg-email">邮箱</label>
            <input id="reg-email" v-model.trim="form.email" type="email" placeholder="选填" />
          </div>
        </div>
        <div class="form-grid">
          <div class="form-item">
            <label for="reg-password">密码 <span class="hint">(6-32 位)</span></label>
            <input id="reg-password" v-model="form.password" type="password" placeholder="请输入密码" autocomplete="new-password" />
          </div>
          <div class="form-item">
            <label for="reg-confirm">确认密码</label>
            <input id="reg-confirm" v-model="form.confirm" type="password" placeholder="再次输入" autocomplete="new-password" />
          </div>
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
import bgImg from '../assets/bg/register.jpg' // 注册页背景：龟背竹露珠实景（本地资源）

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
