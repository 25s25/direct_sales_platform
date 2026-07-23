<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><User /></el-icon>会员管理
      </div>
      <div class="ds-page__header-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>添加会员
        </el-button>
      </div>
    </div>

    <div class="ds-search-bar">
      <el-input
        v-model="searchParams.phone"
        placeholder="手机号"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="searchParams.realName"
        placeholder="姓名"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchParams.level"
        placeholder="会员等级"
        clearable
        @change="handleSearch"
      >
        <el-option
          v-for="level in levelOptions"
          :key="level.id"
          :label="level.name"
          :value="level.id"
        />
      </el-select>
      <el-select
        v-model="searchParams.status"
        placeholder="状态"
        clearable
        @change="handleSearch"
      >
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
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
    >
      <el-table-column prop="memberNo" label="会员编号" width="140" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="levelName" label="等级" width="110">
        <template #default="{ row }">
          <el-tag :type="levelTagType(row.levelName || row.levelId) as any" size="small">
            {{ levelLabel(row.levelId, row.levelName) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="row._statusLoading"
            @change="(val: any) => handleToggleStatus(row, val as boolean)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="walletBalance" label="余额" width="120">
        <template #default="{ row }">
          ¥{{ (row.walletBalance || 0).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            <el-icon><View /></el-icon>查看
          </el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>禁用
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="ds-empty">
          <div class="ds-empty__icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="ds-empty__text">暂无会员数据</div>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '不修改请留空' : '留空默认为 123456'" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="form.levelId" placeholder="请选择会员等级" clearable style="width: 100%;">
            <el-option
              v-for="level in levelOptions"
              :key="level.id"
              :label="level.name"
              :value="level.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐人ID">
          <el-input-number v-model="form.recommendId" :min="0" :controls="false" placeholder="请输入推荐人ID" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { User, Plus, Search, Refresh, View, Edit, Delete } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'member:manage'],
})

const { get, post, put } = useApi()
const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const levelOptions = ref<any[]>([])

const searchParams = reactive({
  phone: '',
  realName: '',
  level: null as number | null,
  status: null as number | null,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加会员')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<any>(null)
const form = reactive({
  phone: '',
  password: '',
  realName: '',
  idCard: '',
  levelId: null as number | null,
  recommendId: null as number | null,
  status: 1,
})

const formRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

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

async function fetchData() {
  loading.value = true
  try {
    const res: any = await get('/api/member/page', {
      page: pagination.page,
      size: pagination.pageSize,
      phone: searchParams.phone || undefined,
      realName: searchParams.realName || undefined,
      level: searchParams.level || undefined,
      status: searchParams.status !== null ? searchParams.status : undefined,
    })
    const data = res.data || res
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取会员列表失败')
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
  searchParams.phone = ''
  searchParams.realName = ''
  searchParams.level = null
  searchParams.status = null
  handleSearch()
}

function resetForm() {
  form.phone = ''
  form.password = ''
  form.realName = ''
  form.idCard = ''
  form.levelId = null
  form.recommendId = null
  form.status = 1
  formRef.value?.resetFields()
}

function handleAdd() {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '添加会员'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑会员'
  resetForm()
  form.phone = row.phone || ''
  form.realName = row.realName || ''
  form.idCard = row.idCard || ''
  form.levelId = row.levelId || null
  form.recommendId = row.recommendId || null
  form.status = row.status ?? 1
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: any = {
      phone: form.phone,
      realName: form.realName || undefined,
      idCard: form.idCard || undefined,
      levelId: form.levelId || undefined,
      recommendId: form.recommendId || undefined,
      status: form.status,
    }
    if (form.password) {
      payload.password = form.password
    }
    if (isEdit.value && editingId.value) {
      await put(`/api/member/admin/${editingId.value}`, payload)
      ElMessage.success('编辑成功')
    } else {
      await post('/api/member/admin', payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row: any, val: boolean) {
  row._statusLoading = true
  try {
    await put(`/api/member/${row.id}/status`, undefined, { params: { status: val ? 1 : 0 } })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    row._statusLoading = false
  }
}

function handleViewDetail(row: any) {
  router.push(`/admin/member/detail?id=${row.id}`)
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确定要禁用会员「${row.realName || row.phone}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await put(`/api/member/${row.id}/status`, undefined, { params: { status: 0 } })
      row.status = 0
      ElMessage.success('已禁用')
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchLevels()
  fetchData()
})
</script>