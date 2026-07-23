<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-button @click="router.back()" text>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        会员详情
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><User /></el-icon>基本信息
      </div>
      <div class="ds-desc-list" v-loading="loading">
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">会员编号</span>
          <span class="ds-desc-list__value">{{ member.memberNo || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">手机号</span>
          <span class="ds-desc-list__value">{{ member.phone || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">姓名</span>
          <span class="ds-desc-list__value">{{ member.realName || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">身份证号</span>
          <span class="ds-desc-list__value">{{ member.idCard || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">等级</span>
          <span class="ds-desc-list__value">
            <el-tag :type="levelTagType(member.levelName || member.levelId) as any" size="small">
              {{ levelLabel(member.levelId, member.levelName) }}
            </el-tag>
          </span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">推荐人</span>
          <span class="ds-desc-list__value">{{ member.recommendName || member.recommendId || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">上级ID</span>
          <span class="ds-desc-list__value">{{ member.parentId || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">注册时间</span>
          <span class="ds-desc-list__value">{{ member.createTime || '-' }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">状态</span>
          <span class="ds-desc-list__value">
            <el-tag :type="member.status === 1 ? 'success' : 'danger'" size="small">
              {{ member.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </span>
        </div>
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><Wallet /></el-icon>钱包信息
      </div>
      <div class="ds-desc-list" v-loading="loading">
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">可用余额</span>
          <span class="ds-desc-list__value">¥{{ (member.walletBalance || 0).toLocaleString() }}</span>
        </div>
        <div class="ds-desc-list__item">
          <span class="ds-desc-list__label">冻结金额</span>
          <span class="ds-desc-list__value">¥{{ (member.frozenAmount || 0).toLocaleString() }}</span>
        </div>
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><DataBoard /></el-icon>团队结构
      </div>
      <div class="ds-team-tree" v-loading="treeLoading">
        <el-tree
          :data="teamTree"
          :props="{ label: 'realName', children: 'children' }"
          node-key="id"
          default-expand-all
        >
          <template #default="{ data }">
            <span style="display: flex; align-items: center; gap: 8px;">
              <span style="font-weight: 500;">{{ data.realName || data.phone }}</span>
              <el-tag :type="levelTagType(data.levelName) as any" size="small">
                {{ data.levelName || '-' }}
              </el-tag>
              <span style="color: #909399; font-size: 12px;">{{ data.joinTime || '' }}</span>
            </span>
          </template>
        </el-tree>
        <div v-if="!treeLoading && teamTree.length === 0" class="ds-empty">
          <div class="ds-empty__text">暂无团队数据</div>
        </div>
      </div>
    </div>

    <div class="ds-detail-section">
      <div class="ds-detail-section__title">
        <el-icon><Document /></el-icon>最近订单
      </div>
      <el-table :data="recentOrders" border stripe v-loading="orderLoading">
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="totalAmount" label="金额" width="120">
          <template #default="{ row }">¥{{ (row.totalAmount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span :class="orderStatusClass(row.status)" class="el-tag el-tag--small">
              {{ orderStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" />
        <template #empty>
          <div class="ds-empty">
            <div class="ds-empty__text">暂无订单数据</div>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ArrowLeft, User, Wallet, DataBoard, Document } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'member:manage'],
})

const { get } = useApi()
const route = useRoute()
const router = useRouter()

const memberId = computed(() => route.query.id as string)

const loading = ref(false)
const treeLoading = ref(false)
const orderLoading = ref(false)

const member = reactive<any>({
  memberNo: '',
  phone: '',
  realName: '',
  idCard: '',
  levelId: null,
  levelName: '',
  recommendId: '',
  recommendName: '',
  parentId: '',
  walletBalance: 0,
  frozenAmount: 0,
  createTime: '',
  status: 1,
})

const teamTree = ref<any[]>([])
const recentOrders = ref<any[]>([])
const levelOptions = ref<any[]>([])

function levelLabel(levelId?: string | number | null, levelName?: string) {
  if (levelName) return levelName
  const option = levelOptions.value.find((l: any) => l.id === levelId)
  return option?.name || levelId || '-'
}

function levelTagType(level: string | number | null | undefined) {
  const name = typeof level === 'string' ? level : levelOptions.value.find((l: any) => l.id === level)?.name
  const map: Record<string, string> = {
    '普通会员': 'info',
    '银卡会员': '',
    '金卡会员': 'warning',
    '钻石会员': 'danger',
  }
  return map[name as string] || 'info'
}

async function fetchLevels() {
  try {
    const res: any = await get('/api/member/level/all')
    levelOptions.value = res.data || res || []
  } catch {
    levelOptions.value = []
  }
}

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

async function fetchMemberDetail() {
  loading.value = true
  try {
    const res: any = await get(`/api/member/${memberId.value}`)
    const data = res.data || res
    Object.assign(member, data)
  } catch (error: any) {
    ElMessage.error(error.message || '获取会员信息失败')
  } finally {
    loading.value = false
  }
}

async function fetchTeamTree() {
  treeLoading.value = true
  try {
    const res: any = await get(`/api/member/${memberId.value}/team`)
    teamTree.value = res.data || res || []
  } catch {
    // silently fail
  } finally {
    treeLoading.value = false
  }
}

async function fetchRecentOrders() {
  orderLoading.value = true
  try {
    const res: any = await get('/api/order/page', {
      memberId: memberId.value ? Number(memberId.value) : undefined,
      page: 1,
      size: 10,
    })
    const data = res.data || res
    recentOrders.value = data.records || data.list || []
  } catch {
    // silently fail
  } finally {
    orderLoading.value = false
  }
}

onMounted(() => {
  fetchLevels()
  if (memberId.value) {
    fetchMemberDetail()
    fetchTeamTree()
    fetchRecentOrders()
  }
})
</script>