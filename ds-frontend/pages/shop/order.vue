<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Document /></el-icon>
        <span>我的订单</span>
      </div>
    </div>

    <div style="margin-bottom: 16px;">
      <el-radio-group v-model="searchStatus" @change="handleSearch">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">待付款</el-radio-button>
        <el-radio-button :value="1">已付款</el-radio-button>
        <el-radio-button :value="2">已发货</el-radio-button>
        <el-radio-button :value="3">已签收</el-radio-button>
        <el-radio-button :value="5">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <el-table v-loading="loading" :data="orderList" stripe>
      <el-table-column prop="orderNo" label="订单编号" width="180" />
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="{ row }">¥{{ (row.totalAmount || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="payAmount" label="实付" width="120">
        <template #default="{ row }">¥{{ (row.payAmount || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <span :class="orderStatusClass(row.status)">
            {{ orderStatusLabel(row.status) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            查看详情
          </el-button>
          <el-button
            v-if="row.status === 0"
            type="warning"
            link
            size="small"
            @click="handlePay(row)"
          >
            付款
          </el-button>
          <el-button
            v-if="row.status === 0"
            type="danger"
            link
            size="small"
            @click="handleCancel(row)"
          >
            取消
          </el-button>
          <el-button
            v-if="row.status === 2"
            type="success"
            link
            size="small"
            @click="handleReceive(row)"
          >
            确认收货
          </el-button>
          <el-button
            v-if="row.status === 3 || row.status === 4"
            type="danger"
            link
            size="small"
            @click="handleReturn(row)"
          >
            申请退货
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="ds-empty">
          <div class="ds-empty__icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="ds-empty__text">暂无订单</div>
          <el-button type="primary" @click="router.push('/shop')">去购物</el-button>
        </div>
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span :class="orderStatusClass(currentOrder.status)">
            {{ orderStatusLabel(currentOrder.status) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="订单金额">
          ¥{{ (currentOrder.totalAmount || 0).toLocaleString() }}
        </el-descriptions-item>
        <el-descriptions-item label="实付金额">
          ¥{{ (currentOrder.payAmount || 0).toLocaleString() }}
        </el-descriptions-item>
        <el-descriptions-item label="PV">{{ currentOrder.totalPv || 0 }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="收货人" :span="2">
          {{ currentOrder.receiverName || '-' }} {{ currentOrder.receiverPhone || '' }}
        </el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">
          {{ currentOrder.receiverAddr || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="物流信息" :span="2">
          <template v-if="currentOrder.expressCompany">
            {{ currentOrder.expressCompany }} - {{ currentOrder.expressNo }}
          </template>
          <template v-else>-</template>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="currentOrder?.items && currentOrder.items.length > 0" style="margin-top: 16px;">
        <div style="font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 2px solid #409eff;">
          订单商品
        </div>
        <el-table :data="currentOrder.items" size="small" stripe>
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="subtotal" label="小计" width="100">
            <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 申请退货对话框 -->
    <el-dialog v-model="returnVisible" title="申请退货" width="500px">
      <el-form :model="returnForm" label-width="100px">
        <el-form-item label="订单编号">
          <span>{{ returnForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="实付金额">
          <span>¥{{ (returnForm.payAmount || 0).toLocaleString() }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number
            v-model="returnForm.refundAmount"
            :min="0"
            :max="returnForm.payAmount"
            :precision="2"
            :step="10"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="退货原因" required>
          <el-input
            v-model="returnForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入退货原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" :loading="returnSubmitting" @click="confirmReturn">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Document } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'shop',
  middleware: 'auth',
})

const { get, put, post } = useApi()
const router = useRouter()

const loading = ref(false)
const orderList = ref<any[]>([])
const searchStatus = ref<number | undefined>(undefined)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const detailVisible = ref(false)
const currentOrder = ref<any>(null)

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

async function fetchOrders() {
  loading.value = true
  try {
    const res: any = await get('/api/order/page', {
      page: pagination.page,
      size: pagination.pageSize,
      status: searchStatus.value !== undefined ? searchStatus.value : undefined,
    })
    const data = res.data || res
    orderList.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchOrders()
}

function handleSizeChange() {
  pagination.page = 1
  fetchOrders()
}

function handlePageChange() {
  fetchOrders()
}

async function handleViewDetail(row: any) {
  try {
    const res: any = await get(`/api/order/${row.id}`)
    currentOrder.value = res.data || res
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单详情失败')
  }
}

async function handlePay(row: any) {
  router.push(`/shop/pay-cashier?orderNo=${row.orderNo}&amount=${(row.payAmount || row.totalAmount || 0).toFixed(2)}`)
}

async function handleCancel(row: any) {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await put(`/api/order/${row.id}/cancel`)
      ElMessage.success('订单已取消')
      fetchOrders()
    } catch (error: any) {
      ElMessage.error(error.message || '取消失败')
    }
  }).catch(() => {})
}

async function handleReceive(row: any) {
  ElMessageBox.confirm('确认已收到商品？', '确认收货', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await put(`/api/order/${row.id}/receive`)
      ElMessage.success('已确认收货')
      fetchOrders()
    } catch (error: any) {
      ElMessage.error(error.message || '确认收货失败')
    }
  }).catch(() => {})
}

const returnVisible = ref(false)
const returnSubmitting = ref(false)
const returnForm = reactive({
  orderId: 0,
  orderNo: '',
  payAmount: 0,
  refundAmount: 0,
  reason: '',
})

function handleReturn(row: any) {
  returnForm.orderId = row.id
  returnForm.orderNo = row.orderNo
  returnForm.payAmount = row.payAmount || row.totalAmount || 0
  returnForm.refundAmount = row.payAmount || row.totalAmount || 0
  returnForm.reason = ''
  returnVisible.value = true
}

async function confirmReturn() {
  if (!returnForm.reason.trim()) {
    ElMessage.warning('请输入退货原因')
    return
  }
  returnSubmitting.value = true
  try {
    await post('/api/order/return/apply', {
      orderId: returnForm.orderId,
      reason: returnForm.reason,
      refundAmount: returnForm.refundAmount,
    })
    ElMessage.success('退货申请已提交，请等待审核')
    returnVisible.value = false
    fetchOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '退货申请失败')
  } finally {
    returnSubmitting.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>