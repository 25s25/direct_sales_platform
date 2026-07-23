<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><ShoppingCart /></el-icon>
        <span>购物车</span>
      </div>
      <div class="ds-page__header-actions">
        <el-button @click="router.push('/shop')">
          <el-icon><ArrowLeft /></el-icon>继续购物
        </el-button>
      </div>
    </div>

    <div v-if="cartItems.length > 0">
      <el-table :data="cartItems" stripe>
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              style="width: 60px; height: 60px; border-radius: 6px;"
              fit="cover"
            />
            <div v-else style="width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 6px; color: #c0c4cc;">
              <el-icon :size="24"><Goods /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ row.memberPrice }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="99"
              size="small"
              @change="updateCart"
            />
          </template>
        </el-table-column>
        <el-table-column label="PV" width="80">
          <template #default="{ row }">{{ (row.pv || 0) * row.quantity }}</template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600;">
              ¥{{ (row.memberPrice * row.quantity).toLocaleString() }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row, $index }">
            <el-button type="danger" link size="small" @click="handleRemove($index)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="ds-cart__summary">
        <div class="ds-cart__total">
          合计（{{ totalQuantity }} 件商品）：<span>¥{{ totalAmount.toLocaleString() }}</span>
        </div>
        <el-button
          class="ds-cart__checkout-btn"
          type="danger"
          @click="checkoutVisible = true"
        >
          立即结算
        </el-button>
      </div>
    </div>

    <div v-else class="ds-empty">
      <div class="ds-empty__icon">
        <el-icon><ShoppingCart /></el-icon>
      </div>
      <div class="ds-empty__text">购物车为空</div>
      <el-button type="primary" @click="router.push('/shop')">去购物</el-button>
    </div>

    <!-- 结算对话框 -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="550px" @close="resetCheckoutForm">
      <el-form ref="checkoutFormRef" :model="checkoutForm" :rules="checkoutRules" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="checkoutForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="checkoutForm.receiverPhone" placeholder="请输入收货人手机号" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input
            v-model="checkoutForm.receiverAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入收货地址"
          />
        </el-form-item>
      </el-form>
      <div style="background: #f5f7fa; border-radius: 8px; padding: 16px; margin-bottom: 8px;">
        <div style="display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 4px;">
          <span>商品总数</span>
          <span>{{ totalQuantity }} 件</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 4px;">
          <span>合计PV</span>
          <span style="color: #e6a23c;">{{ totalPv }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 16px; font-weight: 600;">
          <span>合计金额</span>
          <span style="color: #f56c6c;">¥{{ totalAmount.toLocaleString() }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="danger" :loading="checkoutSubmitting" @click="handleCheckout">
          确认下单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ShoppingCart, ArrowLeft, Goods, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

definePageMeta({
  layout: 'shop',
  middleware: 'auth',
})

const { post } = useApi()
const router = useRouter()

const cartItems = ref<any[]>([])
const checkoutVisible = ref(false)
const checkoutSubmitting = ref(false)
const checkoutFormRef = ref<FormInstance>()
const checkoutForm = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
})

const checkoutRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }],
}

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.memberPrice * item.quantity, 0)
})

const totalPv = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.pv || 0) * item.quantity, 0)
})

function loadCart() {
  try {
    cartItems.value = JSON.parse(localStorage.getItem('shop_cart') || '[]')
  } catch {
    cartItems.value = []
  }
}

function updateCart() {
  localStorage.setItem('shop_cart', JSON.stringify(cartItems.value))
}

function handleRemove(index: number) {
  ElMessageBox.confirm('确定要移除该商品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    cartItems.value.splice(index, 1)
    updateCart()
    ElMessage.success('已移除')
  }).catch(() => {})
}

function resetCheckoutForm() {
  checkoutForm.receiverName = ''
  checkoutForm.receiverPhone = ''
  checkoutForm.receiverAddress = ''
  checkoutFormRef.value?.resetFields()
}

async function handleCheckout() {
  if (!checkoutFormRef.value) return
  const valid = await checkoutFormRef.value.validate().catch(() => false)
  if (!valid) return

  checkoutSubmitting.value = true
  try {
    const items = cartItems.value.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
    }))
    const payload = {
      items,
      receiverName: checkoutForm.receiverName,
      receiverPhone: checkoutForm.receiverPhone,
      receiverAddr: checkoutForm.receiverAddress,
    }
    await post('/api/order/create', payload)
    ElMessage.success('下单成功')
    localStorage.removeItem('shop_cart')
    checkoutVisible.value = false
    router.push('/shop/order')
  } catch (error: any) {
    ElMessage.error(error.message || '下单失败')
  } finally {
    checkoutSubmitting.value = false
  }
}

onMounted(() => {
  loadCart()
})
</script>