<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Money /></el-icon>奖金管理
      </div>
    </div>

    <el-card shadow="hover" style="margin-bottom: 20px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600;">当前奖金方案</span>
          <el-button type="primary" size="small" @click="openPlanDialog">
            <el-icon><SwitchButton /></el-icon>切换方案
          </el-button>
        </div>
      </template>
      <div v-if="currentPlan" class="ds-desc-list">
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">方案名称</span>
          <span class="ds-desc-list__value">{{ currentPlan.name }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">方案类型</span>
          <span class="ds-desc-list__value">
            <span class="el-tag el-tag--small ds-status-tag--active">
              {{ currentPlan.planType || '标准' }}
            </span>
          </span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">状态</span>
          <span class="ds-desc-list__value">
            <span class="el-tag el-tag--small ds-status-tag--active">
              {{ currentPlan.isActive === 1 ? '启用中' : '未启用' }}
            </span>
          </span>
        </div>
      </div>
      <div v-else v-loading="planLoading" class="ds-empty">
        <div class="ds-empty__text">暂无活跃方案</div>
      </div>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600;">奖金记录</span>
          <el-button type="success" size="small" @click="handleGrantBonus">
            <el-icon><Coin /></el-icon>发放奖金
          </el-button>
        </div>
      </template>

      <div style="margin-bottom: 16px;">
        <el-radio-group v-model="searchParams.bonusType" @change="handleSearch">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="RETAIL">零售奖</el-radio-button>
          <el-radio-button value="REFERRAL">推荐奖</el-radio-button>
          <el-radio-button value="BINARY">对碰奖</el-radio-button>
          <el-radio-button value="LEVEL">层奖</el-radio-button>
          <el-radio-button value="LEADERSHIP">领导奖</el-radio-button>
        </el-radio-group>
      </div>

      <div class="ds-search-bar">
        <el-input
          v-model="searchParams.memberId"
          placeholder="会员ID"
          clearable
          @clear="handleSearch"
        />
        <el-date-picker
          v-model="searchParams.period"
          type="month"
          placeholder="结算周期"
          value-format="YYYY-MM"
          @change="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>重置
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="memberId" label="会员ID" width="100" />
        <el-table-column prop="period" label="周期" width="100" />
        <el-table-column label="奖金类型" width="110">
          <template #default="{ row }">
            <span :class="bonusTypeClass(row.bonusType)" class="el-tag el-tag--small">
              {{ bonusTypeLabel(row.bonusType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">¥{{ (row.amount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="sourceOrderNo" label="来源订单" width="160">
          <template #default="{ row }">{{ row.sourceOrderNo || row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'ds-status-tag--granted' : 'ds-status-tag--pending'" class="el-tag el-tag--small">
              {{ row.status === 1 ? '已发放' : '待发放' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <template #empty>
          <div class="ds-empty">
            <div class="ds-empty__text">暂无奖金记录</div>
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
    </el-card>

    <el-dialog v-model="planDialogVisible" title="选择奖金方案" width="500px">
      <el-select
        v-model="selectedPlanId"
        placeholder="请选择奖金方案"
        style="width: 100%"
      >
        <el-option
          v-for="plan in availablePlans"
          :key="plan.id"
          :label="plan.name"
          :value="plan.id"
        />
      </el-select>
      <el-empty v-if="availablePlans.length === 0 && !planLoading" description="没有可选方案" />
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="planSwitching" @click="confirmSwitchPlan">
          确定切换
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grantDialogVisible" title="发放奖金" width="500px">
      <p v-if="selectedRecordIds.length === 0" style="color: #f56c6c;">请先在表格中选择要发放的奖金记录</p>
      <p v-else>已选择 <strong>{{ selectedRecordIds.length }}</strong> 条待发放记录，确定发放？</p>
      <template #footer>
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="grantSubmitting"
          :disabled="selectedRecordIds.length === 0"
          @click="confirmGrant"
        >确定发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Money, SwitchButton, Coin, Search, Refresh } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'bonus:record:manage', 'bonus:plan:manage'],
})

const { get, post, put } = useApi()

const loading = ref(false)
const planLoading = ref(false)
const tableData = ref<any[]>([])

const currentPlan = ref<any>(null)
const availablePlans = ref<any[]>([])
const planDialogVisible = ref(false)
const planSwitching = ref(false)
const selectedPlanId = ref<number | string | null>(null)

const searchParams = reactive({
  memberId: '',
  bonusType: '',
  period: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const grantDialogVisible = ref(false)
const grantSubmitting = ref(false)
const selectedRecordIds = ref<number[]>([])

function bonusTypeLabel(type: string) {
  const map: Record<string, string> = {
    RETAIL: '零售奖',
    REFERRAL: '推荐奖',
    BINARY: '对碰奖',
    LEVEL: '层奖',
    LEADERSHIP: '领导奖',
    REPEAT: '复消奖',
    DIVIDEND: '分红奖',
  }
  return map[type] || type
}

function bonusTypeClass(type: string) {
  const map: Record<string, string> = {
    RETAIL: 'ds-status-tag--active',
    REFERRAL: 'ds-status-tag--paid',
    BINARY: 'ds-status-tag--approved',
    LEVEL: 'ds-status-tag--pending',
    LEADERSHIP: 'ds-status-tag--shipped',
    REPEAT: 'ds-status-tag--granted',
    DIVIDEND: 'ds-status-tag--disabled',
  }
  return map[type] || 'ds-status-tag--pending'
}

async function fetchCurrentPlan() {
  planLoading.value = true
  try {
    const res: any = await get('/api/bonus/plan/active')
    currentPlan.value = res.data || res
  } catch {
    currentPlan.value = null
  } finally {
    planLoading.value = false
  }
}

async function fetchPlans() {
  try {
    const res: any = await get('/api/bonus/plan/all')
    const all = res.data || res || []
    availablePlans.value = all.filter((p: any) => p.id !== currentPlan.value?.id)
  } catch {
    availablePlans.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await get('/api/bonus/record/page', {
      page: pagination.page,
      size: pagination.pageSize,
      memberId: searchParams.memberId ? Number(searchParams.memberId) : undefined,
      bonusType: searchParams.bonusType || undefined,
      period: searchParams.period || undefined,
    })
    const data = res.data || res
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取奖金记录失败')
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
  searchParams.memberId = ''
  searchParams.bonusType = ''
  searchParams.period = ''
  handleSearch()
}

function openPlanDialog() {
  selectedPlanId.value = null
  fetchPlans()
  planDialogVisible.value = true
}

async function confirmSwitchPlan() {
  if (!selectedPlanId.value) {
    ElMessage.warning('请选择一个方案')
    return
  }
  planSwitching.value = true
  try {
    await put(`/api/bonus/plan/switch/${selectedPlanId.value}`)
    ElMessage.success('方案切换成功')
    planDialogVisible.value = false
    selectedPlanId.value = null
    fetchCurrentPlan()
  } catch (error: any) {
    ElMessage.error(error.message || '切换失败')
  } finally {
    planSwitching.value = false
  }
}

function handleSelectionChange(selection: any[]) {
  selectedRecordIds.value = selection.map((row) => row.id).filter(Boolean)
}

function handleGrantBonus() {
  if (selectedRecordIds.value.length === 0) {
    ElMessage.warning('请先在表格中选择要发放的奖金记录')
    return
  }
  grantDialogVisible.value = true
}

async function confirmGrant() {
  grantSubmitting.value = true
  try {
    await put('/api/bonus/record/grant', selectedRecordIds.value)
    ElMessage.success('奖金发放成功')
    grantDialogVisible.value = false
    selectedRecordIds.value = []
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '发放失败')
  } finally {
    grantSubmitting.value = false
  }
}

onMounted(() => {
  fetchCurrentPlan()
  fetchData()
})
</script>