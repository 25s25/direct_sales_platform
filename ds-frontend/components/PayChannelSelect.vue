<template>
  <div class="ds-pay-channel">
    <div
      v-for="item in channels"
      :key="item.value"
      :class="['ds-pay-channel__item', { 'is-active': modelValue === item.value }]"
      @click="handleSelect(item.value)"
    >
      <div class="ds-pay-channel__icon">
        <el-icon :size="24"><component :is="item.icon" /></el-icon>
      </div>
      <div class="ds-pay-channel__info">
        <div class="ds-pay-channel__name">{{ item.label }}</div>
        <div class="ds-pay-channel__desc">{{ item.desc }}</div>
      </div>
      <div class="ds-pay-channel__check">
        <el-icon v-if="modelValue === item.value" color="#409eff" :size="20"><CircleCheck /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Wallet,
  CreditCard,
  ChatDotSquare,
  Money,
  Postcard,
  Coin,
  CircleCheck,
} from '@element-plus/icons-vue'

export type PayChannel =
  | 'WALLET'
  | 'ALIPAY_F2F'
  | 'ALIPAY_WEB'
  | 'WECHAT_NATIVE'
  | 'WECHAT_JSAPI'
  | 'WECHAT_MINIAPP'
  | 'PAYPAL'

defineProps<{
  modelValue?: PayChannel | string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: PayChannel | string): void
}>()

const channels: { value: PayChannel; label: string; desc: string; icon: any }[] = [
  { value: 'WALLET', label: '余额支付', desc: '使用账户余额支付', icon: Wallet },
  { value: 'ALIPAY_F2F', label: '支付宝扫码', desc: '打开支付宝扫一扫付款', icon: CreditCard },
  { value: 'ALIPAY_WEB', label: '支付宝网页', desc: '跳转支付宝网页支付', icon: CreditCard },
  { value: 'WECHAT_NATIVE', label: '微信扫码', desc: '打开微信扫一扫付款', icon: ChatDotSquare },
  { value: 'WECHAT_JSAPI', label: '微信公众号', desc: '在微信内发起支付', icon: Postcard },
  { value: 'WECHAT_MINIAPP', label: '微信小程序', desc: '在小程序中完成支付', icon: Money },
  { value: 'PAYPAL', label: 'PayPal', desc: 'PayPal 国际支付', icon: Coin },
]

function handleSelect(value: PayChannel) {
  emit('update:modelValue', value)
}
</script>

<style scoped lang="scss">
.ds-pay-channel {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.25s ease;
    background: #fff;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
    }

    &.is-active {
      border-color: #409eff;
      background: #f5faff;
    }
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: #ecf5ff;
    color: #409eff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  &__desc {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }

  &__check {
    width: 20px;
    flex-shrink: 0;
  }
}
</style>
