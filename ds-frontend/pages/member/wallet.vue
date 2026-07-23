<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Wallet /></el-icon>
        <span>我的钱包</span>
      </div>
    </div>

    <div class="ds-wallet__balance-card">
      <div class="ds-wallet__balance-card-label">可用余额（元）</div>
      <div class="ds-wallet__balance-card-value">¥{{ (balance || 0).toLocaleString() }}</div>
      <div v-if="frozenAmount > 0" style="font-size: 13px; opacity: 0.8; margin-top: 4px;">
        冻结金额：¥{{ frozenAmount.toLocaleString() }}
      </div>
      <div class="ds-wallet__balance-card-actions">
        <el-button type="primary" @click="withdrawVisible = true">
          <el-icon><Money /></el-icon>提现
        </el-button>
        <el-button @click="handleRecharge">
          <el-icon><CreditCard /></el-icon>充值
        </el-button>
      </div>
    </div>

    <el-card>
      <template #header>
        <span>钱包流水</span>
      </template>

      <el-table v-loading="loading" :data="logList" stripe>
        <el-table-column prop="logType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="walletTypeColor(row.logType)" size="small">
              {{ walletTypeLabel(row.logType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="140">
          <template #default="{ row }">
            <span :style="{ color: (row.amount || 0) >= 0 ? '#67c23a' : '#f56c6c', fontWeight: '600' }">
              {{ (row.amount || 0) >= 0 ? '+' : '' }}¥{{ (row.amount || 0).toLocaleString() }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balanceBefore" label="变动前" width="120">
          <template #default="{ row }">¥{{ (row.balanceBefore || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="变动后" width="120">
          <template #default="{ row }">¥{{ (row.balanceAfter || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <template #empty>
          <div class="ds-empty">
            <div class="ds-empty__icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="ds-empty__text">暂无流水记录</div>
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
    </el-card>

    <!-- 提现对话框 -->
    <el-dialog v-model="withdrawVisible" title="申请提现" width="500px" @close="resetWithdrawForm">
      <el-form ref="withdrawFormRef" :model="withdrawForm" :rules="withdrawRules" label-width="100px">
        <el-form-item label="提现金额" prop="amount">
          <el-input-number
            v-model="withdrawForm.amount"
            :min="1"
            :max="balance"
            :precision="2"
            style="width: 100%"
            placeholder="请输入提现金额"
          />
        </el-form-item>
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="withdrawForm.bankName" placeholder="请输入银行名称" />
        </el-form-item>
        <el-form-item label="银行卡号" prop="bankCard">
          <el-input v-model="withdrawForm.bankCard" placeholder="请输入银行卡号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawVisible = false">取消</el-button>
        <el-button type="primary" :loading="withdrawSubmitting" @click="submitWithdraw">
          确认提现
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Wallet, Money, CreditCard, Document } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

definePageMeta({
  layout: 'member',
  middleware: 'auth',
})

const { get, post } = useApi()
const router = useRouter()

const loading = ref(false)
const balance = ref(0)
const frozenAmount = ref(0)
const logList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

// 提现
const withdrawVisible = ref(false)
const withdrawSubmitting = ref(false)
const withdrawFormRef = ref<FormInstance>()
const withdrawForm = reactive({
  amount: 0,
  bankName: '',
  bankCard: '',
})

const withdrawRules: FormRules = {
  amount: [
    { required: true, message: '请输入提现金额', trigger: 'blur' },
    {
      validator: (_rule, value: number, callback) => {
        if (value <= 0) {
          callback(new Error('提现金额必须大于0'))
        } else if (value > balance.value) {
          callback(new Error('提现金额不能超过可用余额'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  bankCard: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
}

function walletTypeLabel(type: string) {
  const map: Record<string, string> = {
    RECHARGE: '充值',
    CONSUME: '消费',
    BONUS: '奖金',
    WITHDRAW: '提现',
    REFUND: '退款',
  }
  return map[type] || type || '-'
}

function walletTypeColor(type: string): any {
  const map: Record<string, string> = {
    RECHARGE: 'success',
    CONSUME: 'danger',
    BONUS: 'warning',
    WITHDRAW: 'info',
    REFUND: '',
  }
  return map[type] || 'info'
}

async function fetchBalance() {
  try {
    const res: any = await get('/api/member/info')
    const data = res.data || res
    balance.value = data.walletBalance || 0
    frozenAmount.value = data.frozenAmount || 0
  } catch {
    // silently fail
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res: any = await get('/api/finance/wallet/logs/my', {
      page: pagination.page,
      size: pagination.pageSize,
    })
    const data = res.data || res
    logList.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取流水记录失败')
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  pagination.page = 1
  fetchLogs()
}

function handlePageChange() {
  fetchLogs()
}

function resetWithdrawForm() {
  withdrawForm.amount = 0
  withdrawForm.bankName = ''
  withdrawForm.bankCard = ''
  withdrawFormRef.value?.resetFields()
}

async function submitWithdraw() {
  if (!withdrawFormRef.value) return
  const valid = await withdrawFormRef.value.validate().catch(() => false)
  if (!valid) return

  withdrawSubmitting.value = true
  try {
    await post('/api/finance/withdraw/apply', {
      amount: withdrawForm.amount,
      bankName: withdrawForm.bankName,
      bankCard: withdrawForm.bankCard,
    })
    ElMessage.success('提现申请已提交，请等待审核')
    withdrawVisible.value = false
    fetchBalance()
    fetchLogs()
  } catch (error: any) {
    ElMessage.error(error.message || '提现申请失败')
  } finally {
    withdrawSubmitting.value = false
  }
}

async function handleRecharge() {
  ElMessageBox.prompt('请输入充值金额', '充值', {
    confirmButtonText: '去支付',
    cancelButtonText: '取消',
    inputPattern: /^\d+(\.\d{1,2})?$/,
    inputErrorMessage: '请输入正确的金额',
    inputValue: '100',
  }).then(async ({ value }) => {
    const amount = Number(value)
    if (amount <= 0) {
      ElMessage.warning('充值金额必须大于0')
      return
    }
    try {
      const res: any = await post('/api/pay/recharge', { amount })
      if (res.code !== 200) {
        ElMessage.error(res.message || '创建充值订单失败')
        return
      }
      router.push(`/shop/pay-cashier?orderNo=${res.data}&amount=${amount.toFixed(2)}`)
    } catch (e) {
      ElMessage.error('创建充值订单失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchBalance()
  fetchLogs()
})
</script>