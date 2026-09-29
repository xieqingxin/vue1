<template>
  <div class="notice-page">
    <header class="topbar">
      <div class="topbar__left">
        <button class="back-btn" @click="goBack">← 返回个人中心</button>
        <div class="brand">
          <span class="brand__dot" aria-hidden="true"><el-icon><Bell /></el-icon></span>
          <span>公告</span>
        </div>
      </div>
    </header>

    <main class="container">
      <section class="notice-list">
        <el-timeline>
          <el-timeline-item v-for="n in notices" :key="n.id" :timestamp="n.date" placement="top">
            <el-card shadow="never" class="notice-card">
              <div class="notice-card__head">
                <h3>{{ n.title }}</h3>
                <el-tag size="small" :type="n.tagType" effect="light">{{ n.tag }}</el-tag>
              </div>
              <p class="notice-card__body">{{ n.body }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </section>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'

const router = useRouter()

// 平台公告（静态示例数据）
const notices = [
  {
    id: 1,
    date: '2026-09-18',
    title: '添加任务页升级日期选择器',
    tag: '更新',
    tagType: 'primary',
    body: '添加任务页的开始/结束时间已替换为 Element Plus 日期时间选择器，并已汉化，选择体验更佳。'
  },
  {
    id: 2,
    date: '2026-09-17',
    title: '后端数据层迁移到 MyBatis',
    tag: '更新',
    tagType: 'primary',
    body: '数据访问层已由 JdbcTemplate 重构为 Mapper 接口 + XML 映射，代码结构更清晰，后续扩展更方便。'
  },
  {
    id: 3,
    date: '2026-09-13',
    title: '任务状态变更规则',
    tag: '规则',
    tagType: 'warning',
    body: '已完成的任务不可再变更为未完成；超过截止时间仍未完成的任务将自动置为已过期。'
  },
  {
    id: 4,
    date: '2026-09-10',
    title: '自律计划任务平台上线',
    tag: '新功能',
    tagType: 'success',
    body: '平台正式上线，支持登录注册、按类型添加任务、设定起止时间、标记完成，以及个人中心统计与完成日历。'
  }
]

function goBack() {
  router.push('/Home')
}
</script>

<style scoped>
.notice-page {
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

/* ---------- 公告时间线 ---------- */
.notice-list {
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.notice-list :deep(.el-timeline-item__timestamp) {
  color: var(--ink-faint);
  font-size: 12.5px;
}

.notice-card {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.notice-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.notice-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.notice-card__head h3 {
  font-size: 15.5px;
  font-weight: 650;
  color: var(--ink);
}

.notice-card__body {
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-soft);
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}
</style>
