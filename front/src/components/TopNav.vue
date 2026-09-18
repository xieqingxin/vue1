<template>
  <nav class="top-nav">
    <el-menu :default-active="activeMenu" router mode="horizontal" :ellipsis="false">
      <el-menu-item index="/home">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </el-menu-item>
      <el-menu-item index="/announcements">
        <el-icon><Bell /></el-icon>
        <span>公告</span>
      </el-menu-item>
      <el-menu-item index="/about">
        <el-icon><InfoFilled /></el-icon>
        <span>关于</span>
      </el-menu-item>
    </el-menu>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Bell, InfoFilled } from '@element-plus/icons-vue'

// 顶部导航：跟随当前路由高亮，配合 el-menu 的 router 模式跳转
const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.top-nav :deep(.el-menu--horizontal) {
  --el-menu-active-color: var(--accent-deep);
  --el-menu-hover-bg-color: var(--accent-soft);
  --el-menu-hover-text-color: var(--accent-deep);
  --el-menu-bg-color: transparent;
  --el-menu-horizontal-height: 62px;
  border-bottom: none;
}

.top-nav :deep(.el-menu-item) {
  height: 62px;
  padding: 0 16px;
  font-weight: 550;
  color: var(--ink-soft);
}

.top-nav :deep(.el-menu-item.is-active) {
  font-weight: 650;
}
</style>
