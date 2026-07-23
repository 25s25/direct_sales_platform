<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><House /></el-icon>
        <span>{{ greeting }}</span>
      </div>
    </div>

    <div class="ds-stat-grid">
      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--blue">
          <el-icon><Wallet /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">我的余额</div>
          <div class="ds-stat-card__value">¥{{ (memberStore.member.walletBalance || 0).toLocaleString() }}</div>
        </div>
      </div>
      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--green">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">累计PV</div>
          <div class="ds-stat-card__value">{{ (memberStore.member.totalPv || 0).toLocaleString() }}</div>
        </div>
      </div>
      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--orange">
          <el-icon><User /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">团队人数</div>
          <div class="ds-stat-card__value">{{ (memberStore.member.teamCount || 0).toLocaleString() }}</div>
        </div>
      </div>
      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--purple">
          <el-icon><Money /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">本月奖金</div>
          <div class="ds-stat-card__value">¥{{ (memberStore.member.monthBonus || 0).toLocaleString() }}</div>
        </div>
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><Document /></el-icon>
        <span>基本信息</span>
        <el-button type="primary" link style="margin-left: auto;" @click="openProfileDialog">
          编辑资料
        </el-button>
      </div>
      <div class="ds-desc-list" v-loading="loading">
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">头像</span>
          <span class="ds-desc-list__value">
            <ImageUpload v-model="memberStore.member.avatar" module="member" :biz-id="memberStore.member.id || ''" @update:model-value="handleAvatarUpdate" />
          </span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">会员编号</span>
          <span class="ds-desc-list__value">{{ memberStore.member.memberNo || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">手机号</span>
          <span class="ds-desc-list__value">{{ memberStore.member.phone || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">邮箱</span>
          <span class="ds-desc-list__value">{{ memberStore.member.email || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">手机验证</span>
          <span class="ds-desc-list__value">
            <el-tag :type="memberStore.member.phoneVerified ? 'success' : 'info'" size="small">
              {{ memberStore.member.phoneVerified ? '已验证' : '未验证' }}
            </el-tag>
          </span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">邮箱验证</span>
          <span class="ds-desc-list__value">
            <el-tag :type="memberStore.member.emailVerified ? 'success' : 'info'" size="small">
              {{ memberStore.member.emailVerified ? '已验证' : '未验证' }}
            </el-tag>
          </span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">真实姓名</span>
          <span class="ds-desc-list__value">{{ memberStore.member.realName || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">会员等级</span>
          <span class="ds-desc-list__value">
            <el-tag :type="levelTagType(memberStore.member.levelName || memberStore.member.levelId)" size="small">
              {{ levelLabel(memberStore.member.levelId, memberStore.member.levelName) }}
            </el-tag>
          </span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">注册时间</span>
          <span class="ds-desc-list__value">{{ memberStore.member.createTime || '-' }}</span>
        </div>
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><Lock /></el-icon>
        <span>账号绑定</span>
      </div>
      <div class="ds-binding-list">
        <div class="ds-binding-list__item">
          <div class="ds-binding-list__info">
            <el-icon><Message /></el-icon>
            <div>
              <div class="ds-binding-list__name">邮箱绑定</div>
              <div class="ds-binding-list__value">{{ memberStore.member.email || '未绑定' }}</div>
            </div>
          </div>
          <el-button type="primary" link @click="openBindDialog('email')">
            {{ memberStore.member.email ? '修改' : '绑定' }}
          </el-button>
        </div>
        <div class="ds-binding-list__item">
          <div class="ds-binding-list__info">
            <el-icon><Phone /></el-icon>
            <div>
              <div class="ds-binding-list__name">手机绑定</div>
              <div class="ds-binding-list__value">{{ memberStore.member.phone || '未绑定' }}</div>
            </div>
          </div>
          <el-button type="primary" link @click="openBindDialog('phone')">
            {{ memberStore.member.phone ? '修改' : '绑定' }}
          </el-button>
        </div>
        <div class="ds-binding-list__item">
          <div class="ds-binding-list__info">
            <el-icon><ChatDotSquare /></el-icon>
            <div>
              <div class="ds-binding-list__name">微信绑定</div>
              <div class="ds-binding-list__value">{{ memberStore.member.wechatBound ? '已绑定' : '未绑定' }}</div>
            </div>
          </div>
          <el-button
            :type="memberStore.member.wechatBound ? 'danger' : 'primary'"
            link
            @click="handleSocialBind('wechat_web')"
          >
            {{ memberStore.member.wechatBound ? '解绑' : '绑定' }}
          </el-button>
        </div>
        <div class="ds-binding-list__item">
          <div class="ds-binding-list__info">
            <el-icon><Briefcase /></el-icon>
            <div>
              <div class="ds-binding-list__name">企业微信绑定</div>
              <div class="ds-binding-list__value">{{ memberStore.member.workWechatBound ? '已绑定' : '未绑定' }}</div>
            </div>
          </div>
          <el-button
            :type="memberStore.member.workWechatBound ? 'danger' : 'primary'"
            link
            @click="handleSocialBind('workwechat')"
          >
            {{ memberStore.member.workWechatBound ? '解绑' : '绑定' }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><ShoppingCart /></el-icon>
        <span>最近订单</span>
      </div>
      <el-table v-loading="orderLoading" :data="recentOrders" stripe>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="totalAmount" label="金额" width="120">
          <template #default="{ row }">¥{{ (row.totalAmount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span :class="orderStatusClass(row.status)">
              {{ orderStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" />
        <template #empty>
          <div class="ds-empty">
            <div class="ds-empty__icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="ds-empty__text">暂无订单</div>
          </div>
        </template>
      </el-table>
    </div>

    <!-- 绑定对话框 -->
    <el-dialog v-model="bindDialogVisible" :title="bindDialogTitle" width="420px" @close="resetBindForm">
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="90px">
        <el-form-item :label="bindType === 'email' ? '邮箱' : '手机号'" prop="target">
          <el-input
            v-model="bindForm.target"
            :placeholder="bindType === 'email' ? '请输入邮箱' : '请输入手机号'"
            clearable
          />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="bindForm.code" placeholder="请输入验证码" clearable maxlength="6">
            <template #append>
              <el-button :disabled="bindCountdown > 0" @click="handleSendBindCode">
                {{ bindCountdown > 0 ? `${bindCountdown}s` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitBind">确认</el-button>
      </template>
    </el-dialog>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="profileDialogVisible" title="编辑资料" width="420px" @close="resetProfileForm">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileSubmitting" @click="submitProfileUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  House,
  Wallet,
  TrendCharts,
  User,
  Money,
  Document,
  ShoppingCart,
  Lock,
  Message,
  Phone,
  ChatDotSquare,
  Briefcase,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useMemberStore } from '~/stores/member'

definePageMeta({
  layout: 'member',
  middleware: 'auth',
})

const authStore = useAuthStore()
const memberStore = useMemberStore()
const { get, post, put } = useApi()

const loading = ref(false)
const orderLoading = ref(false)
const levelOptions = ref<any[]>([])
const recentOrders = ref<any[]>([])

const bindDialogVisible = ref(false)
const bindType = ref<'email' | 'phone'>('email')
const bindSubmitting = ref(false)
const bindFormRef = ref<FormInstance>()
const bindForm = reactive({
  target: '',
  code: '',
})
const bindCountdown = ref(0)
let bindTimer: ReturnType<typeof setInterval> | null = null

const bindDialogTitle = computed(() => (bindType.value === 'email' ? '绑定邮箱' : '绑定手机'))

const bindRules: FormRules = {
  target: [{ required: true, message: '请输入邮箱/手机号', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const profileDialogVisible = ref(false)
const profileSubmitting = ref(false)
const profileFormRef = ref<FormInstance>()
const profileForm = reactive({
  phone: '',
  realName: '',
})

const profileRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  const name = memberStore.member.realName || authStore.user?.realName || '会员'
  let prefix = '上午好'
  if (hour >= 6 && hour < 12) prefix = '上午好'
  else if (hour >= 12 && hour < 14) prefix = '中午好'
  else if (hour >= 14 && hour < 18) prefix = '下午好'
  else prefix = '晚上好'
  return `${prefix}，${name}，欢迎来到会员中心`
})

function levelLabel(levelId?: string | number | null, levelName?: string) {
  if (levelName) return levelName
  const option = levelOptions.value.find((l: any) => l.id === levelId)
  return option?.name || levelId || '-'
}

function levelTagType(level: string | number | null | undefined): any {
  const name = typeof level === 'string' ? level : levelOptions.value.find((l: any) => l.id === level)?.name
  const map: Record<string, string> = {
    '普通会员': 'info',
    '银卡会员': '',
    '金卡会员': 'warning',
    '钻石会员': 'danger',
  }
  return map[name as string] || 'info'
}

async function fetchLevels() {
  try {
    const res: any = await get('/api/member/level/all')
    levelOptions.value = res.data || res || []
  } catch {
    levelOptions.value = []
  }
}

function orderStatusLabel(status: number) {
  const map: Record<number, string> = {
    0: '待付款',
    1: '已付款',
    2: '已发货',
    3: '已签收',
    5: '已取消',
  }
  return map[status] || status || '-'
}

function orderStatusClass(status: number) {
  const map: Record<number, string> = {
    0: 'ds-status-tag--pending',
    1: 'ds-status-tag--paid',
    2: 'ds-status-tag--shipped',
    3: 'ds-status-tag--received',
    5: 'ds-status-tag--cancelled',
  }
  return `ds-status-tag ${map[status] || ''}`
}

async function fetchMemberInfo() {
  loading.value = true
  try {
    await memberStore.fetchMember()
    authStore.user = {
      ...authStore.user,
      realName: memberStore.member.realName || '',
      avatar: memberStore.member.avatar || '',
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取会员信息失败')
  } finally {
    loading.value = false
  }
}

async function handleAvatarUpdate(url: string) {
  if (!url || url === memberStore.member.avatar) return
  try {
    await put('/api/member/update', { avatar: url })
    authStore.user = {
      ...authStore.user,
      avatar: url,
    }
    ElMessage.success('头像更新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '头像保存失败')
  }
}

async function fetchRecentOrders() {
  orderLoading.value = true
  try {
    const res: any = await get('/api/order/page', { page: 1, size: 5 })
    const data = res.data || res
    recentOrders.value = data.records || data.list || []
  } catch {
    // silently fail
  } finally {
    orderLoading.value = false
  }
}

function openBindDialog(type: 'email' | 'phone') {
  bindType.value = type
  bindForm.target = type === 'email' ? (memberStore.member.email || '') : (memberStore.member.phone || '')
  bindForm.code = ''
  bindDialogVisible.value = true
}

function resetBindForm() {
  bindForm.target = ''
  bindForm.code = ''
  bindFormRef.value?.resetFields()
  if (bindTimer) {
    clearInterval(bindTimer)
    bindTimer = null
  }
  bindCountdown.value = 0
}

async function handleSendBindCode() {
  if (!bindForm.target) {
    ElMessage.warning('请先输入邮箱/手机号')
    return
  }
  const type = bindType.value === 'email' ? 'email' : 'sms'
  try {
    await post('/api/captcha/send', {
      type,
      scene: 'bind',
      target: bindForm.target,
    })
    ElMessage.success('验证码已发送')
    bindCountdown.value = 60
    bindTimer = setInterval(() => {
      bindCountdown.value--
      if (bindCountdown.value <= 0 && bindTimer) {
        clearInterval(bindTimer)
        bindTimer = null
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  }
}

async function submitBind() {
  if (!bindFormRef.value) return
  const valid = await bindFormRef.value.validate().catch(() => false)
  if (!valid) return

  bindSubmitting.value = true
  try {
    const verifyRes: any = await post('/api/captcha/verify', {
      type: bindType.value === 'email' ? 'email' : 'sms',
      scene: 'bind',
      target: bindForm.target,
      code: bindForm.code,
    })
    const verified = verifyRes.data ?? verifyRes
    if (verified !== true) {
      ElMessage.error('验证码错误或已过期')
      bindSubmitting.value = false
      return
    }
    const payload: Record<string, any> = {}
    if (bindType.value === 'email') {
      payload.email = bindForm.target
    } else {
      payload.phone = bindForm.target
    }
    await put('/api/member/update', payload)
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
    fetchMemberInfo()
  } catch (error: any) {
    ElMessage.error(error.message || '绑定失败')
  } finally {
    bindSubmitting.value = false
  }
}

async function handleSocialBind(type: string) {
  const boundKey = type === 'wechat_web' ? 'wechatBound' : 'workWechatBound'
  const isBound = memberStore.member[boundKey]

  if (isBound) {
    try {
      await ElMessageBox.confirm('确定要解绑吗？', '提示', { type: 'warning' })
      await post('/api/auth/social/unbind', { type })
      ElMessage.success('解绑成功')
      fetchMemberInfo()
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '解绑失败')
      }
    }
  } else {
    try {
      const redirectUri = `${window.location.origin}/auth/social-callback?type=${type}&mode=bind`
      const res: any = await get('/api/auth/social/url', { type, redirectUri })
      const url = res.data || res
      if (url && typeof url === 'string') {
        window.location.href = url
      } else {
        ElMessage.error('未获取到授权链接')
      }
    } catch (error: any) {
      ElMessage.error(error.message || '获取授权链接失败')
    }
  }
}

function openProfileDialog() {
  profileForm.phone = memberStore.member.phone || ''
  profileForm.realName = memberStore.member.realName || ''
  profileDialogVisible.value = true
}

function resetProfileForm() {
  profileForm.phone = ''
  profileForm.realName = ''
  profileFormRef.value?.resetFields()
}

async function submitProfileUpdate() {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  profileSubmitting.value = true
  try {
    await put('/api/member/update', {
      phone: profileForm.phone,
      realName: profileForm.realName,
    })
    ElMessage.success('资料更新成功')
    profileDialogVisible.value = false
    fetchMemberInfo()
  } catch (error: any) {
    ElMessage.error(error.message || '资料更新失败')
  } finally {
    profileSubmitting.value = false
  }
}

onMounted(() => {
  fetchLevels()
  fetchMemberInfo()
  fetchRecentOrders()
})
</script>

<style scoped lang="scss">
.ds-binding-list {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;
    &:last-child { border-bottom: none; }
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 12px;
    .el-icon { font-size: 20px; color: #409eff; }
  }

  &__name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  &__value {
    font-size: 13px;
    color: #909399;
    margin-top: 2px;
  }
}
</style>
