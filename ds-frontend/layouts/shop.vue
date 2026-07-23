<template>
  <el-container class="ds-layout ds-layout--shop">
    <el-aside width="220px" class="ds-layout__aside">
      <div class="ds-layout__logo">
        <div class="ds-layout__logo-icon">
          <el-icon><ShoppingCart /></el-icon>
        </div>
        <h2>产品商城</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :router="true"
        background-color="transparent"
        text-color="#a8b5c6"
        active-text-color="#fff"
      >
        <template v-for="item in shopMenuItems" :key="item.path">
          <el-menu-item :index="item.path">
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
              <el-dropdown-item command="member">
                <el-icon><User /></el-icon>会员中心
              </el-dropdown-item>
              <el-dropdown-item command="cart">
                <el-icon><ShoppingCart /></el-icon>购物车
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
  ShoppingCart,
  Tickets,
  Document,
  ArrowDown,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

onMounted(() => {
  authStore.initFromStorage('member')
})

const activeMenu = computed(() => route.path)

const shopMenuItems = [
  { path: '/shop', title: '产品商城', icon: ShoppingCart },
  { path: '/shop/cart', title: '购物车', icon: Tickets },
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
  } else if (command === 'member') {
    router.push('/member')
  } else if (command === 'cart') {
    router.push('/shop/cart')
  }
}
</script>
