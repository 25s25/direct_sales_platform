<template>
  <div class="ds-pay-qrcode">
    <div v-if="dataUrl" class="ds-pay-qrcode__image">
      <img :src="dataUrl" alt="支付宝二维码" />
    </div>
    <div v-else class="ds-pay-qrcode__empty">二维码加载中...</div>
    <p class="ds-pay-qrcode__tip">请使用支付宝扫一扫完成支付</p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import * as QRCode from 'qrcode'

const props = defineProps<{
  qrCode: string
}>()

const dataUrl = ref('')

async function generate() {
  if (!props.qrCode) {
    dataUrl.value = ''
    return
  }
  try {
    dataUrl.value = await QRCode.toDataURL(props.qrCode, {
      width: 220,
      margin: 2,
      color: { dark: '#1677FF', light: '#fff' },
    })
  } catch {
    dataUrl.value = ''
  }
}

watch(() => props.qrCode, generate, { immediate: true })
</script>

<style scoped lang="scss">
.ds-pay-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;

  &__image {
    width: 220px;
    height: 220px;
    padding: 12px;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 8px;

    img {
      width: 100%;
      height: 100%;
    }
  }

  &__empty {
    width: 220px;
    height: 220px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #909399;
    background: #f5f7fa;
    border-radius: 8px;
  }

  &__tip {
    margin-top: 12px;
    font-size: 13px;
    color: #606266;
  }
}
</style>
