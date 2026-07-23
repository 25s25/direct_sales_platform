<template>
  <div class="landing-page">
    <div class="landing-container">
      <h1>直销管理系统</h1>
      <p class="subtitle">Direct Selling Platform</p>
      <div class="nav-links">
        <NuxtLink to="/shop" class="nav-card">
          <el-icon class="nav-card__icon"><ShoppingCart /></el-icon>
          <span class="label">前台商城</span>
        </NuxtLink>
        <NuxtLink to="/member" class="nav-card">
          <el-icon class="nav-card__icon"><User /></el-icon>
          <span class="label">会员中心</span>
        </NuxtLink>
        <NuxtLink v-if="showAdmin" to="/admin" class="nav-card">
          <el-icon class="nav-card__icon"><Setting /></el-icon>
          <span class="label">后台管理</span>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ShoppingCart, User, Setting } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'blank',
})

const authStore = useAuthStore()

// Only show admin card if user is logged in and has admin permissions
const showAdmin = computed(() => {
  if (!authStore.isLoggedIn) return false
  const permissions = authStore.user?.permissions || []
  return permissions.some((p: string) => p === 'system:manage')
})
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.landing-container {
  text-align: center;
  color: white;
}
.landing-container h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}
.subtitle {
  font-size: 1.2rem;
  opacity: 0.8;
  margin-bottom: 3rem;
}
.nav-links {
  display: flex;
  gap: 2rem;
  justify-content: center;
  flex-wrap: wrap;
}
.nav-card {
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 2rem;
  width: 160px;
  text-decoration: none;
  color: white;
  transition: transform 0.2s, background 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.nav-card:hover {
  transform: translateY(-4px);
  background: rgba(255,255,255,0.25);
}
.nav-card__icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
}
.label {
  font-size: 1.1rem;
  font-weight: 500;
}
</style>
