<template>
  <div class="about-page">
    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goBack">← 返回个人中心</button>
        <div class="brand">
          <span class="brand__dot" aria-hidden="true"><el-icon><InfoFilled /></el-icon></span>
          <span>关于</span>
        </div>
      </div>
    </header>

    <main class="container">
      <el-card shadow="never" class="hero-card">
        <div class="about-hero">
          <h1>自律计划任务平台</h1>
          <p>把每一个小目标贴成便利贴，一件一件完成它们。</p>
          <div class="about-hero__tags">
            <el-tag type="success" effect="light">v0.1.0</el-tag>
            <el-tag type="primary" effect="light">个人项目</el-tag>
          </div>
        </div>
      </el-card>

      <section class="info-cards">
        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">🎯 项目简介</span>
          </template>
          <p>
            这是一个「登录注册 + 任务管理」的个人项目：支持按类型添加任务、设定起止时间、标记完成，
            并配有个人中心的统计面板与任务完成日历，还集成了文件上传功能。
          </p>
        </el-card>

        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">🧰 技术栈</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="前端">Vue 3 · Vite · Pinia · Vue Router · Element Plus</el-descriptions-item>
            <el-descriptions-item label="后端">Spring Boot 2.7 · MyBatis · JWT · BCrypt</el-descriptions-item>
            <el-descriptions-item label="数据库">MySQL 8</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">💬 说明</span>
          </template>
          <el-alert title="本项目为个人开发项目" type="info" :closable="false" show-icon />
        </el-card>
      </section>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()

function goBack() {
  router.push('/Home')
}
</script>

<style scoped>
.about-page {
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
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 44px) clamp(16px, 4vw, 24px) 72px;
}

/* ---------- 内容卡片 ---------- */
.hero-card {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.05s both;
}

.hero-card :deep(.el-card__body) {
  padding: clamp(26px, 4vw, 40px);
}

.about-hero {
  text-align: center;
}

.about-hero h1 {
  font-size: clamp(22px, 4vw, 30px);
  font-weight: 700;
  letter-spacing: -0.015em;
  margin-bottom: 8px;
}

.about-hero p {
  color: var(--ink-soft);
  font-size: 14.5px;
  margin-bottom: 16px;
}

.about-hero__tags {
  display: inline-flex;
  gap: 8px;
}

.info-cards {
  margin-top: 20px;
  display: grid;
  gap: 16px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.12s both;
}

.info-card {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.info-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--line);
  padding: 13px 20px;
}

.info-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.card-title {
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.info-card p {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-soft);
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}
</style>
