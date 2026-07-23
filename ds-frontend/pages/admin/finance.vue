<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Wallet /></el-icon>财务管理
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="提现管理" name="withdraw">
        <div class="ds-search-bar">
          <el-select
            v-model="withdrawParams.status"
            placeholder="审核状态"
            clearable
            @change="fetchWithdrawList"
          >
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已打款" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
          <el-button type="primary" @click="fetchWithdrawList">
            <el-icon><Search /></el-icon>搜索
          </el-button>
        </div>

        <el-table v-loading="withdrawLoading" :data="withdrawList" border stripe>
          <el-table-column prop="withdrawNo" label="提现单号" width="180" />
          <el-table-column prop="memberId" label="会员ID" width="100" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">¥{{ (row.amount || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="fee" label="手续费" width="100">
            <template #default="{ row }">¥{{ (row.fee || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="实到" width="120">
            <template #default="{ row }">¥{{ ((row.amount || 0) - (row.fee || 0)).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="bankName" label="银行" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span :class="withdrawStatusClass(row.status)" class="el-tag el-tag--small">
                {{ withdrawStatusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button type="success" link size="small" @click="handleAudit(row, 1)">
                  <el-icon><Check /></el-icon>通过
                </el-button>
                <el-button type="danger" link size="small" @click="handleAudit(row, 3)">
                  <el-icon><Close /></el-icon>拒绝
                </el-button>
              </template>
              <el-button
                v-if="row.status === 1"
                type="primary"
                link
                size="small"
                @click="handleGrant(row)"
              >
                <el-icon><Money /></el-icon>打款
              </el-button>
              <span v-else-if="row.status === 3" style="color: #909399;">已拒绝</span>
              <span v-else-if="row.status === 2" style="color: #67c23a;">已打款</span>
            </template>
          </el-table-column>
          <template #empty>
            <div class="ds-empty">
              <div class="ds-empty__text">暂无提现记录</div>
            </div>
          </template>
        </el-table>

        <el-pagination
          v-model:current-page="withdrawPagination.page"
          v-model:page-size="withdrawPagination.pageSize"
          :total="withdrawPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </el-tab-pane>

      <el-tab-pane label="钱包流水" name="walletLog">
        <div class="ds-search-bar">
          <el-input
            v-model="walletParams.memberId"
            placeholder="会员ID"
            clearable
            @clear="fetchWalletLogs"
          />
          <el-select
            v-model="walletParams.type"
            placeholder="流水类型"
            clearable
            @change="fetchWalletLogs"
          >
            <el-option label="充值" value="RECHARGE" />
            <el-option label="消费" value="CONSUME" />
            <el-option label="奖金" value="BONUS" />
            <el-option label="提现" value="WITHDRAW" />
            <el-option label="退款" value="REFUND" />
          </el-select>
          <el-button type="primary" @click="fetchWalletLogs">
            <el-icon><Search /></el-icon>搜索
          </el-button>
        </div>

        <el-table v-loading="walletLoading" :data="walletLogs" border stripe>
          <el-table-column prop="memberId" label="会员ID" width="100" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <span :class="walletTypeClass(row.logType)" class="el-tag el-tag--small">
                {{ walletTypeLabel(row.logType) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span :style="{ color: (row.amount || 0) >= 0 ? '#67c23a' : '#f56c6c' }">
                {{ (row.amount || 0) >= 0 ? '+' : '' }}¥{{ (row.amount || 0).toLocaleString() }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动前" width="120">
            <template #default="{ row }">¥{{ (row.balanceBefore || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="变动后" width="120">
            <template #default="{ row }">¥{{ (row.balanceAfter || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" />
          <el-table-column prop="createTime" label="时间" width="170" />
          <template #empty>
            <div class="ds-empty">
              <div class="ds-empty__text">暂无流水记录</div>
            </div>
          </template>
        </el-table>

        <el-pagination
          v-model:current-page="walletPagination.page"
          v-model:page-size="walletPagination.pageSize"
          :total="walletPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="auditDialogVisible" title="提现审核" width="500px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="3">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入审核备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="confirmAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Wallet, Search, Check, Close, Money } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'finance:manage'],
})

const { get, put } = useApi()

const activeTab = ref('withdraw')

const withdrawLoading = ref(false)
const withdrawList = ref<any[]>([])

const withdrawParams = reactive({ status: null as number | null })

const withdrawPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const walletLoading = ref(false)
const walletLogs = ref<any[]>([])

const walletParams = reactive({
  memberId: '',
  type: '',
})

const walletPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const auditDialogVisible = ref(false)
const auditSubmitting = ref(false)
const auditingRow = ref<any>(null)
const auditForm = reactive({
  status: 1 as number,
  remark: '',
})

function withdrawStatusLabel(status: number) {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已通过',
    2: '已打款',
    3: '已拒绝',
  }
  return map[status] || status
}

function withdrawStatusClass(status: number) {
  const map: Record<number, string> = {
    0: 'ds-status-tag--pending',
    1: 'ds-status-tag--approved',
    2: 'ds-status-tag--granted',
    3: 'ds-status-tag--rejected',
  }
  return map[status] || ''
}

function walletTypeLabel(type: string) {
  const map: Record<string, string> = {
    RECHARGE: '充值',
    CONSUME: '消费',
    BONUS: '奖金',
    WITHDRAW: '提现',
    REFUND: '退款',
  }
  return map[type] || type
}

function walletTypeClass(type: string) {
  const map: Record<string, string> = {
    RECHARGE: 'ds-status-tag--active',
    CONSUME: 'ds-status-tag--disabled',
    BONUS: 'ds-status-tag--pending',
    WITHDRAW: 'ds-status-tag--paid',
    REFUND: 'ds-status-tag--granted',
  }
  return map[type] || ''
}

async function fetchWithdrawList() {
  withdrawLoading.value = true
  try {
    const res: any = await get('/api/finance/withdraw/page', {
      page: withdrawPagination.page,
      size: withdrawPagination.pageSize,
      status: withdrawParams.status !== null ? withdrawParams.status : undefined,
    })
    const data = res.data || res
    withdrawList.value = data.records || data.list || []
    withdrawPagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取提现列表失败')
  } finally {
    withdrawLoading.value = false
  }
}

async function fetchWalletLogs() {
  walletLoading.value = true
  try {
    const res: any = await get('/api/finance/wallet/logs', {
      page: walletPagination.page,
      size: walletPagination.pageSize,
      memberId: walletParams.memberId || undefined,
      type: walletParams.type || undefined,
    })
    const data = res.data || res
    walletLogs.value = data.records || data.list || []
    walletPagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取钱包流水失败')
  } finally {
    walletLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  if (tab === 'withdraw') {
    fetchWithdrawList()
  } else if (tab === 'walletLog') {
    fetchWalletLogs()
  }
}

function handleAudit(row: any, status: number) {
  auditingRow.value = row
  auditForm.status = status
  auditForm.remark = ''
  auditDialogVisible.value = true
}

async function confirmAudit() {
  if (!auditingRow.value) return
  auditSubmitting.value = true
  try {
    await put(`/api/finance/withdraw/${auditingRow.value.id}/audit`, undefined, { params: { status: auditForm.status, remark: auditForm.remark || undefined } })
    const action = auditForm.status === 1 ? '已通过' : '已拒绝'
    ElMessage.success(action)
    auditDialogVisible.value = false
    fetchWithdrawList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    auditSubmitting.value = false
  }
}

async function handleGrant(row: any) {
  ElMessageBox.confirm('确认已打款给该会员吗？', '确认打款', {
    confirmButtonText: '确认打款',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await put(`/api/finance/withdraw/${row.id}/grant`)
      ElMessage.success('打款成功')
      fetchWithdrawList()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    }
  }).catch(() => {})
}

function handleSizeChange() {
  if (activeTab.value === 'withdraw') {
    withdrawPagination.page = 1
    fetchWithdrawList()
  } else if (activeTab.value === 'walletLog') {
    walletPagination.page = 1
    fetchWalletLogs()
  }
}

function handlePageChange() {
  if (activeTab.value === 'withdraw') {
    fetchWithdrawList()
  } else if (activeTab.value === 'walletLog') {
    fetchWalletLogs()
  }
}

onMounted(() => {
  fetchWithdrawList()
})
</script>