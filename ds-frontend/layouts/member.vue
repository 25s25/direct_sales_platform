<template>
  <el-container class="ds-layout ds-layout--member">
    <el-aside width="220px" class="ds-layout__aside">
      <div class="ds-layout__logo">
        <div class="ds-layout__logo-icon">
          <el-icon><User /></el-icon>
        </div>
        <h2>会员中心</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :router="true"
        background-color="transparent"
        text-color="#b8c5d6"
        active-text-color="#fff"
      >
        <template v-for="item in memberMenuItems" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="ds-layout__header">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="ds-layout__user">
            <el-avatar :size="32" :src="authStore.user.avatar || ''">
              {{ authStore.user.realName?.charAt(0) || '会' }}
            </el-avatar>
            <span class="ds-layout__user-name">{{ authStore.user.realName || '会员' }}</span>
            <el-icon><ArrowDown class="ds-layout__user-arrow" /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人信息
              </el-dropdown-item>
              <el-dropdown-item command="shop">
                <el-icon><ShoppingCart /></el-icon>前往商城
              </el-dropdown-item>
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
  User,
  Wallet,
  ArrowDown,
  SwitchButton,
  ShoppingCart,
  HomeFilled,
  Document,
  Connection,
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

onMounted(() => {
  authStore.initFromStorage('member')
})

const activeMenu = computed(() => route.path)

const memberMenuItems: Array<{path: string; title: string; icon: any; children?: Array<{path: string; title: string}>}> = [
  { path: '/member', title: '会员首页', icon: HomeFilled },
  { path: '/member/team', title: '我的团队', icon: Connection },
  { path: '/member/wallet', title: '我的钱包', icon: Wallet },
  { path: '/shop/order', title: '我的订单', icon: Document },
]

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
  } else if (command === 'profile') {
    router.push('/member')
  } else if (command === 'shop') {
    router.push('/shop')
  }
}
</script>
