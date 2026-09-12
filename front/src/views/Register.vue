<template>
  <div class="auth-page">
    <img
      class="auth-bg"
      src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=fresh%20dewdrops%20on%20bright%20green%20monstera%20leaves%20in%20soft%20daylight%2C%20clean%20minimal%20botanical%20photography%2C%20light%20mint%20and%20white%20tones&image_size=landscape_16_9"
      alt=""
    />
    <div class="auth-brand">
      <span class="auth-brand__dot">见</span>
      <span>用户中心</span>
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
