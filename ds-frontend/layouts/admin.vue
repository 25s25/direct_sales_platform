<template>
  <el-container class="ds-layout ds-layout--admin">
    <el-aside width="220px" class="ds-layout__aside">
      <div class="ds-layout__logo">
        <div class="ds-layout__logo-icon">
          <el-icon><DataBoard /></el-icon>
        </div>
        <h2>管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :router="true"
        background-color="transparent"
        text-color="#a8b5c6"
        active-text-color="#fff"
      >
        <template v-for="item in adminMenuItems" :key="item.path">
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
              {{ authStore.user.realName?.charAt(0) || '管' }}
            </el-avatar>
            <span class="ds-layout__user-name">{{ authStore.user.realName || '管理员' }}</span>
            <el-icon><ArrowDown class="ds-layout__user-arrow" /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="member">
                <el-icon><HomeFilled /></el-icon>会员中心
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
  DataBoard,
  User,
  Goods,
  List,
  Wallet,
  Setting,
  ArrowDown,
  SwitchButton,
  HomeFilled,
  Coin,
  Document,
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

onMounted(() => {
  authStore.initFromStorage('admin')
})

const activeMenu = computed(() => route.path)

const adminMenuItems: any[] = [
  { path: '/admin', title: '数据概览', icon: DataBoard },
  {
    path: '/admin/member',
    title: '会员管理',
    icon: User,
    children: [
      { path: '/admin/member', title: '会员列表' },
    ],
  },
  { path: '/admin/product', title: '产品管理', icon: Goods },
  { path: '/admin/order', title: '订单管理', icon: List },
  { path: '/admin/bonus', title: '奖金管理', icon: Coin },
  { path: '/admin/finance', title: '财务管理', icon: Wallet },
  { path: '/admin/system', title: '系统管理', icon: Setting },
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
  }
}
</script>
