<template>
  <div class="ds-login">
    <div class="ds-login__card">
      <div class="ds-login__header">
        <h2>直销管理系统</h2>
        <p>欢迎回来，请登录您的账号</p>
      </div>

      <el-tabs v-model="activeTab" stretch class="ds-login__tabs">
        <el-tab-pane label="管理员登录" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="0"
            size="large"
            @submit.prevent="handlePasswordLogin"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="passwordForm.phone"
                placeholder="请输入管理员用户名"
                :prefix-icon="Phone"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
                class="ds-login__btn"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="会员登录" name="code">
          <el-form
            ref="codeFormRef"
            :model="codeForm"
            :rules="codeRules"
            label-width="0"
            size="large"
            @submit.prevent="handleCodeLogin"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="codeForm.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                clearable
                maxlength="11"
              />
            </el-form-item>
            <!-- 密码登录 -->
            <el-form-item v-if="memberLoginMode === 'password'" prop="password">
              <el-input
                v-model="codeForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <!-- 验证码登录 -->
            <el-form-item v-if="memberLoginMode === 'code'" prop="code">
              <el-input
                v-model="codeForm.code"
                placeholder="请输入短信验证码"
                :prefix-icon="Message"
                clearable
                maxlength="6"
              >
                <template #append>
                  <el-button
                    :disabled="codeCountdown > 0"
                    @click="handleSendCode('codeLogin')"
                  >
                    {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <div class="ds-login__toggle">
              <el-button type="primary" link @click="memberLoginMode = memberLoginMode === 'password' ? 'code' : 'password'">
                {{ memberLoginMode === 'password' ? '切换到验证码登录' : '切换到密码登录' }}
              </el-button>
            </div>
            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
                class="ds-login__btn"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-width="0"
            size="large"
            @submit.prevent="handleRegister"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                clearable
                maxlength="11"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请设置密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item prop="code">
              <el-input
                v-model="registerForm.code"
                placeholder="请输入短信验证码"
                :prefix-icon="Message"
                clearable
                maxlength="6"
              >
                <template #append>
                  <el-button
                    :disabled="registerCountdown > 0"
                    @click="handleSendCode('register')"
                  >
                    {{ registerCountdown > 0 ? `${registerCountdown}s` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱（可选）"
                :prefix-icon="MessageBox"
                clearable
              />
            </el-form-item>
            <el-form-item prop="inviteCode">
              <el-input
                v-model="registerForm.inviteCode"
                placeholder="请输入邀请码（可选）"
                :prefix-icon="Ticket"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
                class="ds-login__btn"
              >
                {{ loading ? '注册中...' : '注册' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="ds-login__social">
        <el-divider>第三方登录</el-divider>
        <div class="ds-login__social-btns">
          <el-button circle size="large" @click="handleSocialLogin('wechat_web')">
            <el-icon :size="20" color="#07C160"><ChatDotSquare /></el-icon>
          </el-button>
          <el-button circle size="large" @click="handleSocialLogin('workwechat')">
            <el-icon :size="20" color="#2F7EED"><Briefcase /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Phone, Lock, Message, MessageBox, Ticket, ChatDotSquare, Briefcase } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

definePageMeta({
  layout: 'blank',
})

const authStore = useAuthStore()
const memberStore = useMemberStore()
const router = useRouter()
const { post, get } = useApi()

const activeTab = ref('password')
const loading = ref(false)

const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  phone: '',
  password: '',
})
const passwordRules: FormRules = {
  phone: [
    { required: true, message: '请输入管理员用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

const memberLoginMode = ref<'password' | 'code'>('password')

const codeFormRef = ref<FormInstance>()
const codeForm = reactive({
  phone: '',
  password: '',
  code: '',
})
const codeRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur', validator: (_r: any, _v: any, cb: any) => { if (memberLoginMode.value !== 'password') cb(); else if (!codeForm.password) cb(new Error('请输入密码')); else if (codeForm.password.length < 6) cb(new Error('密码长度不能少于6位')); else cb(); } },
  ],
  code: [
    { required: true, message: '请输入短信验证码', trigger: 'blur', validator: (_r: any, _v: any, cb: any) => { if (memberLoginMode.value !== 'code') cb(); else if (!codeForm.code) cb(new Error('请输入短信验证码')); else cb(); } },
  ],
}

const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  phone: '',
  password: '',
  code: '',
  email: '',
  inviteCode: '',
})
const registerRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入短信验证码', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

const codeCountdown = ref(0)
const registerCountdown = ref(0)
let codeTimer: ReturnType<typeof setInterval> | null = null
let registerTimer: ReturnType<typeof setInterval> | null = null

async function handlePasswordLogin() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.adminLogin(passwordForm.phone, passwordForm.password)
    console.log('[Login] Admin token saved, navigating to /admin...')
    ElMessage.success('登录成功')
    await router.push('/admin')
    console.log('[Login] Navigation completed, current path:', window.location.pathname)
  } catch (error: any) {
    console.error('[Login] Error:', error)
    ElMessage.error(error.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}

async function handleCodeLogin() {
  if (!codeFormRef.value) return
  const valid = await codeFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload: Record<string, string> = { phone: codeForm.phone }
    if (memberLoginMode.value === 'password') {
      payload.password = codeForm.password
    } else {
      payload.code = codeForm.code
    }
    const password = memberLoginMode.value === 'password' ? codeForm.password : undefined
    const code = memberLoginMode.value === 'code' ? codeForm.code : undefined
    await authStore.memberLogin(codeForm.phone, password, code)
    try {
      await memberStore.fetchMember()
    } catch {
      // ignore
    }
    ElMessage.success('登录成功')
    router.push('/member')
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerFormRef.value) return
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await post('/api/member/register', {
      phone: registerForm.phone,
      password: registerForm.password,
      code: registerForm.code,
      email: registerForm.email || undefined,
      inviteCode: registerForm.inviteCode || undefined,
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'password'
    passwordForm.phone = registerForm.phone
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

async function handleSendCode(scene: 'codeLogin' | 'register') {
  const phone = scene === 'codeLogin' ? codeForm.phone : registerForm.phone
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    await post('/api/captcha/send', {
      type: 'sms',
      scene: scene === 'codeLogin' ? 'login' : 'register',
      target: phone,
    })
    ElMessage.success('验证码已发送')
    startCountdown(scene)
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  }
}

function startCountdown(scene: 'codeLogin' | 'register') {
  if (scene === 'codeLogin') {
    codeCountdown.value = 60
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0 && codeTimer) {
        clearInterval(codeTimer)
        codeTimer = null
      }
    }, 1000)
  } else {
    registerCountdown.value = 60
    registerTimer = setInterval(() => {
      registerCountdown.value--
      if (registerCountdown.value <= 0 && registerTimer) {
        clearInterval(registerTimer)
        registerTimer = null
      }
    }, 1000)
  }
}

async function handleSocialLogin(type: string) {
  try {
    const redirectUri = `${window.location.origin}/auth/social-callback?type=${type}`
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
</script>

<style scoped lang="scss">
.ds-login {
  &__toggle {
    text-align: right;
    margin-top: -8px;
    margin-bottom: 4px;
  }

  &__tabs {
    :deep(.el-tabs__nav-wrap::after) {
      background-color: transparent;
    }
  }

  &__social {
    margin-top: 24px;

    &-btns {
      display: flex;
      justify-content: center;
      gap: 16px;
      margin-top: 12px;
    }
  }
}
</style>
