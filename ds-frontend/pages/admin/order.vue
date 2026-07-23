<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Document /></el-icon>订单管理
      </div>
    </div>

    <el-tabs v-model="activeTab" class="ds-order-tabs">
      <el-tab-pane label="订单列表" name="orders" />
      <el-tab-pane label="退货审核" name="returns" />
    </el-tabs>

    <!-- 订单列表 -->
    <div v-show="activeTab === 'orders'">
    <div class="ds-search-bar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="订单编号/会员信息"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-date-picker
        v-model="searchParams.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><Refresh /></el-icon>重置
      </el-button>
    </div>

    <div style="margin-bottom: 16px;">
      <el-radio-group v-model="searchParams.status" @change="handleSearch">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">待付款</el-radio-button>
        <el-radio-button :value="1">已付款</el-radio-button>
        <el-radio-button :value="2">已发货</el-radio-button>
        <el-radio-button :value="3">已签收</el-radio-button>
        <el-radio-button :value="5">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
    >
      <el-table-column prop="orderNo" label="订单编号" width="180" />
      <el-table-column label="会员" width="140">
        <template #default="{ row }">
          <div>{{ row.memberName || row.memberPhone || '-' }}</div>
          <div style="font-size: 12px; color: #909399;">{{ row.memberPhone || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="{ row }">¥{{ (row.totalAmount || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="payAmount" label="实付" width="120">
        <template #default="{ row }">¥{{ (row.payAmount || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="totalPv" label="PV" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span :class="orderStatusClass(row.status)" class="el-tag el-tag--small">
            {{ orderStatusLabel(row.status) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            <el-icon><View /></el-icon>查看
          </el-button>
          <el-button
            v-if="row.status === 1"
            type="success"
            link
            size="small"
            @click="handleShip(row)"
          >
            <el-icon><Upload /></el-icon>发货
          </el-button>
          <el-button
            v-if="row.status === 0 || row.status === 1"
            type="danger"
            link
            size="small"
            @click="handleCancel(row)"
          >
            <el-icon><Close /></el-icon>取消
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="ds-empty">
          <div class="ds-empty__icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="ds-empty__text">暂无订单数据</div>
        </div>
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
    </div>

    <!-- 退货审核 -->
    <div v-show="activeTab === 'returns'">
      <div style="margin-bottom: 16px;">
        <el-radio-group v-model="returnSearchStatus" @change="handleReturnSearch">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="1">已通过</el-radio-button>
          <el-radio-button :value="2">已拒绝</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="returnLoading" :data="returnList" border stripe>
        <el-table-column prop="returnNo" label="退货单号" width="180" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column prop="reason" label="退货原因" show-overflow-tooltip />
        <el-table-column prop="refundAmount" label="退款金额" width="120">
          <template #default="{ row }">¥{{ (row.refundAmount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="returnStatusTagType(row.status)" size="small">
              {{ returnStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column prop="auditRemark" label="审核备注" show-overflow-tooltip />
        <el-table-column prop="auditTime" label="审核时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              size="small"
              @click="handleAuditReturn(row, 1)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              link
              size="small"
              @click="handleAuditReturn(row, 2)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="ds-empty">
            <div class="ds-empty__icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="ds-empty__text">暂无退货申请</div>
          </div>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="returnPagination.page"
        v-model:page-size="returnPagination.pageSize"
        :total="returnPagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleReturnSizeChange"
        @current-change="handleReturnPageChange"
      />
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span :class="orderStatusClass(currentOrder.status)" class="el-tag el-tag--small">
            {{ orderStatusLabel(currentOrder.status) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="会员">{{ currentOrder.memberName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentOrder.memberPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ (currentOrder.totalAmount || 0).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">¥{{ (currentOrder.payAmount || 0).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="PV">{{ currentOrder.totalPv || 0 }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.receiverAddr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货人" :span="2">{{ currentOrder.receiverName || '-' }} {{ currentOrder.receiverPhone || '' }}</el-descriptions-item>
        <el-descriptions-item label="物流信息" :span="2">
          <template v-if="currentOrder.expressCompany">
            {{ currentOrder.expressCompany }} - {{ currentOrder.expressNo }}
          </template>
          <template v-else>-</template>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="currentOrder?.items && currentOrder.items.length > 0" style="margin-top: 16px;">
        <div class="ds-detail-section__title">
          <el-icon><Goods /></el-icon>订单商品
        </div>
        <el-table :data="currentOrder.items" size="small" border>
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

    <el-dialog v-model="shipVisible" title="发货" width="500px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="物流公司" required>
          <el-input v-model="shipForm.expressCompany" placeholder="请输入物流公司" />
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input v-model="shipForm.expressNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="confirmShip">确定发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import { Document, Search, Refresh, View, Upload, Close, Goods } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'order:manage'],
})

const { get, put, post } = useApi()

const loading = ref(false)
const tableData = ref<any[]>([])
const activeTab = ref('orders')

const searchParams = reactive({
  keyword: '',
  status: undefined as number | undefined,
  dateRange: null as string[] | null,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const detailVisible = ref(false)
const currentOrder = ref<any>(null)

const shipVisible = ref(false)
const shipSubmitting = ref(false)
const shippingOrder = ref<any>(null)
const shipForm = reactive({
  expressCompany: '',
  expressNo: '',
})

function orderStatusLabel(status: number) {
  const map: Record<number, string> = {
    0: '待付款',
    1: '已付款',
    2: '已发货',
    3: '已签收',
    5: '已取消',
  }
  return map[status] || status
}

function orderStatusClass(status: number) {
  const map: Record<number, string> = {
    0: 'ds-status-tag--pending',
    1: 'ds-status-tag--paid',
    2: 'ds-status-tag--shipped',
    3: 'ds-status-tag--received',
    5: 'ds-status-tag--cancelled',
  }
  return map[status] || ''
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchParams.keyword || undefined,
      status: searchParams.status !== undefined ? searchParams.status : undefined,
    }
    if (searchParams.dateRange && searchParams.dateRange.length === 2) {
      params.startDate = searchParams.dateRange[0]
      params.endDate = searchParams.dateRange[1]
    }
    const res: any = await get('/api/order/admin/page', params)
    const data = res.data || res
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleSizeChange() {
  pagination.page = 1
  fetchData()
}

function handlePageChange() {
  fetchData()
}

function handleReset() {
  searchParams.keyword = ''
  searchParams.status = undefined
  searchParams.dateRange = null
  handleSearch()
}

async function handleViewDetail(row: any) {
  try {
    const res: any = await get(`/api/order/admin/${row.id}`)
    currentOrder.value = res.data || res
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单详情失败')
  }
}

function handleShip(row: any) {
  shippingOrder.value = row
  shipForm.expressCompany = ''
  shipForm.expressNo = ''
  shipVisible.value = true
}

async function confirmShip() {
  if (!shipForm.expressCompany || !shipForm.expressNo) {
    ElMessage.warning('请填写物流信息')
    return
  }
  shipSubmitting.value = true
  try {
    await put(`/api/order/admin/${shippingOrder.value.id}/ship`, undefined, {
      params: {
        expressCompany: shipForm.expressCompany,
        expressNo: shipForm.expressNo,
      },
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '发货失败')
  } finally {
    shipSubmitting.value = false
  }
}

function handleCancel(row: any) {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await put(`/api/order/admin/${row.id}/cancel`)
      ElMessage.success('订单已取消')
      fetchData()
    } catch (error: any) {
      ElMessage.error(error.message || '取消失败')
    }
  }).catch(() => {})
}

// ---- 退货审核 ----
const returnLoading = ref(false)
const returnList = ref<any[]>([])
const returnSearchStatus = ref<number | undefined>(undefined)
const returnPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

function returnStatusLabel(status: number) {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝',
  }
  return map[status] || '-'
}

function returnStatusTagType(status: number): any {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger',
  }
  return map[status] || 'info'
}

async function fetchReturns() {
  returnLoading.value = true
  try {
    const res: any = await get('/api/order/return/page', {
      page: returnPagination.page,
      size: returnPagination.pageSize,
      status: returnSearchStatus.value !== undefined ? returnSearchStatus.value : undefined,
    })
    const data = res.data || res
    returnList.value = data.records || data.list || []
    returnPagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取退货列表失败')
  } finally {
    returnLoading.value = false
  }
}

function handleReturnSearch() {
  returnPagination.page = 1
  fetchReturns()
}

function handleReturnSizeChange() {
  returnPagination.page = 1
  fetchReturns()
}

function handleReturnPageChange() {
  fetchReturns()
}

function handleAuditReturn(row: any, status: number) {
  const action = status === 1 ? '通过' : '拒绝'
  ElMessageBox.prompt(`请输入审核备注（可选）`, `确认${action}退货`, {
    confirmButtonText: `确认${action}`,
    cancelButtonText: '取消',
    inputPlaceholder: '审核备注',
    inputType: 'textarea',
  }).then(async ({ value }) => {
    try {
      await post('/api/order/return/audit', {
        returnId: row.id,
        status,
        remark: value || '',
      })
      ElMessage.success(`已${action}退货申请`)
      fetchReturns()
    } catch (error: any) {
      ElMessage.error(error.message || `审核失败`)
    }
  }).catch(() => {})
}

// 监听 tab 切换
watch(activeTab, (val) => {
  if (val === 'returns' && returnList.value.length === 0) {
    fetchReturns()
  }
})

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.ds-order-tabs {
  margin-bottom: 16px;
}
</style>