<template>
  <div class="ds-login">
    <div class="ds-login__card" style="text-align: center;">
      <div class="ds-login__header">
        <h2>社交登录</h2>
        <p>{{ statusText }}</p>
      </div>
      <el-icon v-if="loading" class="is-loading" :size="40" color="#409eff">
        <Loading />
      </el-icon>
      <el-button v-else type="primary" @click="router.push('/login')">
        返回登录
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'blank',
})

const route = useRoute()
const router = useRouter()
const { post } = useApi()
const authStore = useAuthStore()
const memberStore = useMemberStore()

const loading = ref(true)
const statusText = ref('正在处理登录...')

onMounted(async () => {
  const type = route.query.type as string
  const code = route.query.code as string

  if (!type || !code) {
    statusText.value = '缺少授权信息'
    loading.value = false
    return
  }

  const mode = route.query.mode as string

  try {
    if (mode === 'bind') {
      await post('/api/auth/social/bind', { type, code })
      ElMessage.success('绑定成功')
      router.push('/member')
    } else {
      const res: any = await post('/api/auth/social/login', { type, code })
      const data = res.data || res
      const token = data.token || data.accessToken || ''
      if (token) {
        authStore.setToken(token)
        try {
          await memberStore.fetchMember()
        } catch {
          // ignore
        }
        ElMessage.success('登录成功')
        const redirect = route.query.redirect as string
        router.push(redirect || '/member')
      } else {
        statusText.value = '登录失败，未获取到授权信息'
        loading.value = false
      }
    }
  } catch (error: any) {
    statusText.value = error.message || (mode === 'bind' ? '绑定失败' : '登录失败')
    loading.value = false
  }
})
</script>

<style scoped>
.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
