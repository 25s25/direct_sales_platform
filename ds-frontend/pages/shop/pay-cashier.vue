<template>
  <div class="ds-page ds-pay-cashier">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><CreditCard /></el-icon>
        <span>收银台</span>
      </div>
    </div>

    <el-card class="ds-pay-cashier__amount-card">
      <div class="ds-pay-cashier__label">订单编号：{{ orderNo || '-' }}</div>
      <div class="ds-pay-cashier__amount">
        <span class="ds-pay-cashier__amount-unit">¥</span>
        {{ Number(amount || 0).toFixed(2) }}
      </div>
      <div class="ds-pay-cashier__amount-desc">请确认订单金额并选择支付方式</div>
    </el-card>

    <el-card class="ds-pay-cashier__channel-card">
      <template #header>
        <span>选择支付方式</span>
      </template>
      <PayChannelSelect v-model="channel" />
    </el-card>

    <div class="ds-pay-cashier__submit">
      <el-button
        type="primary"
        size="large"
        :loading="paying"
        :disabled="!channel"
        @click="handlePay"
      >
        {{ paying ? '支付中...' : '立即支付' }}
      </el-button>
      <el-button size="large" @click="router.back()">取消</el-button>
    </div>

    <el-dialog
      v-model="qrcodeVisible"
      title="扫码支付"
      width="360px"
      align-center
      :close-on-click-modal="false"
      @close="stopQuery"
    >
      <AlipayQrcode v-if="qrcodeType === 'alipay'" :qr-code="qrcodeValue" />
      <WechatQrcode v-else-if="qrcodeType === 'wechat'" :code-url="qrcodeValue" />
      <div class="ds-pay-cashier__query-status">
        <el-icon v-if="querying"><Loading /></el-icon>
        <span>{{ queryStatusText }}</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { CreditCard, Loading } from '@element-plus/icons-vue'
import type { PayChannel } from '~/components/PayChannelSelect.vue'

definePageMeta({
  layout: 'shop',
  middleware: 'auth',
})

const route = useRoute()
const router = useRouter()
const { post, get } = useApi()

const orderNo = computed(() => route.query.orderNo as string)
const amount = computed(() => route.query.amount as string)

const channel = ref<PayChannel | string>('')
const paying = ref(false)

const qrcodeVisible = ref(false)
const qrcodeType = ref<'alipay' | 'wechat' | ''>('')
const qrcodeValue = ref('')
const currentPayOrderNo = ref('')
const querying = ref(false)
const queryStatusText = ref('等待支付...')

let queryTimer: ReturnType<typeof setInterval> | null = null

onUnmounted(() => {
  stopQuery()
})

async function handlePay() {
  if (!channel.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  if (!orderNo.value) {
    ElMessage.error('订单号不存在')
    return
  }

  paying.value = true
  try {
    const extra: Record<string, string> = {}
    const res: any = await post('/api/pay/create', {
      orderNo: orderNo.value,
      channel: channel.value,
      extra,
    })
    const data = res.data || res
    handlePayResult(data)
  } catch (error: any) {
    ElMessage.error(error.message || '支付发起失败')
  } finally {
    paying.value = false
  }
}

function handlePayResult(data: any) {
  currentPayOrderNo.value = data.payOrderNo || ''
  switch (channel.value) {
    case 'WALLET':
      if (data.success && data.status === 2) {
        ElMessage.success('支付成功')
        router.push('/shop/order')
      } else {
        ElMessage.error(data.msg || '支付失败，请检查钱包余额')
      }
      break
    case 'ALIPAY_F2F':
      if (!data.qrCode) {
        ElMessage.error('未获取到支付二维码')
        return
      }
      qrcodeType.value = 'alipay'
      qrcodeValue.value = data.qrCode
      qrcodeVisible.value = true
      startQuery()
      break
    case 'WECHAT_NATIVE':
      if (!data.codeUrl) {
        ElMessage.error('未获取到支付二维码')
        return
      }
      qrcodeType.value = 'wechat'
      qrcodeValue.value = data.codeUrl
      qrcodeVisible.value = true
      startQuery()
      break
    case 'ALIPAY_WEB':
      if (data.formHtml) {
        const div = document.createElement('div')
        div.innerHTML = data.formHtml
        document.body.appendChild(div)
        const form = div.querySelector('form')
        if (form) form.submit()
      } else {
        ElMessage.error('未获取到支付表单')
      }
      break
    case 'WECHAT_JSAPI':
      if (data.jsapiParams && (window as any).WeixinJSBridge) {
        ;(window as any).WeixinJSBridge.invoke(
          'getBrandWCPayRequest',
          data.jsapiParams,
          (payRes: any) => {
            if (payRes.err_msg === 'get_brand_wcpay_request:ok') {
              ElMessage.success('支付成功')
              router.push('/shop/order')
            } else if (payRes.err_msg === 'get_brand_wcpay_request:cancel') {
              ElMessage.info('用户取消支付')
            } else {
              ElMessage.error('支付失败：' + payRes.err_msg)
            }
          }
        )
      } else {
        ElMessage.warning('请在微信客户端内完成支付')
      }
      break
    case 'WECHAT_MINIAPP':
      ElMessage.info('请在小程序中完成支付')
      break
    case 'PAYPAL':
      if (data.approvalUrl) {
        window.location.href = data.approvalUrl
      } else {
        ElMessage.error('未获取到 PayPal 支付链接')
      }
      break
    default:
      ElMessage.info('支付方式处理中')
  }
}

function startQuery() {
  stopQuery()
  if (!currentPayOrderNo.value) return
  querying.value = true
  queryStatusText.value = '等待支付...'
  queryTimer = setInterval(async () => {
    try {
      const res: any = await get(`/api/pay/query/${currentPayOrderNo.value}`)
      const data = res.data || res
      const status = data.status
      if (status === 2) {
        stopQuery()
        ElMessage.success('支付成功')
        qrcodeVisible.value = false
        router.push('/shop/order')
      } else if (status === 3) {
        stopQuery()
        queryStatusText.value = '订单已关闭或支付失败'
      }
    } catch {
      // ignore
    }
  }, 3000)
}

function stopQuery() {
  if (queryTimer) {
    clearInterval(queryTimer)
    queryTimer = null
  }
  querying.value = false
}
</script>

<style scoped lang="scss">
.ds-pay-cashier {
  &__amount-card {
    text-align: center;
    margin-bottom: 20px;
  }

  &__label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }

  &__amount {
    font-size: 48px;
    font-weight: 700;
    color: #f56c6c;
    line-height: 1.2;
  }

  &__amount-unit {
    font-size: 28px;
    margin-right: 4px;
  }

  &__amount-desc {
    font-size: 13px;
    color: #909399;
    margin-top: 8px;
  }

  &__channel-card {
    margin-bottom: 20px;
  }

  &__submit {
    display: flex;
    justify-content: center;
    gap: 16px;
  }

  &__query-status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 12px;
    font-size: 13px;
    color: #606266;

    .el-icon {
      animation: ds-spin 1s linear infinite;
    }
  }
}

@keyframes ds-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
