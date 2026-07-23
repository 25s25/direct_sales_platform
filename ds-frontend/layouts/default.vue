<template>
  <el-container class="ds-layout">
    <el-aside width="220px" class="ds-layout__aside">
      <div class="ds-layout__logo">
        <div class="ds-layout__logo-icon">
          <el-icon><DataBoard /></el-icon>
        </div>
        <h2>直销系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :router="true"
        background-color="transparent"
        text-color="#a8b5c6"
        active-text-color="#fff"
      >
        <slot name="sidebar" />
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="ds-layout__header">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="ds-layout__user">
            <el-avatar :size="32" :src="authStore.user.avatar || ''">
              {{ authStore.user.realName?.charAt(0) || '用' }}
            </el-avatar>
            <span class="ds-layout__user-name">{{ authStore.user.realName || '用户' }}</span>
            <el-icon><ArrowDown class="ds-layout__user-arrow" /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="ds-layout__main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {
  DataBoard,
  ArrowDown,
  SwitchButton,
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

onMounted(() => {
  authStore.initFromStorage('member')
})

const activeMenu = computed(() => route.path)

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      authStore.logout()
      router.push('/login')
    })
  }
}
</script>