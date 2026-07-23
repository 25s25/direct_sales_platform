<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Goods /></el-icon>产品管理
      </div>
      <div class="ds-page__header-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>添加产品
        </el-button>
      </div>
    </div>

    <div class="ds-search-bar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="产品编号/名称"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchParams.categoryId"
        placeholder="产品分类"
        clearable
        @change="handleSearch"
      >
        <el-option
          v-for="cat in categoryList"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
        />
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
      <el-table-column prop="productNo" label="产品编号" width="120" />
      <el-table-column label="图片" width="100">
        <template #default="{ row }">
          <el-image
            v-if="row.mainImage"
            :src="row.mainImage"
            style="width: 60px; height: 60px"
            fit="cover"
            :preview-src-list="[row.mainImage]"
            preview-teleported
          />
          <span v-else style="color: #c0c4cc; font-size: 12px;">暂无图片</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="retailPrice" label="零售价" width="100">
        <template #default="{ row }">¥{{ (row.retailPrice || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="memberPrice" label="会员价" width="100">
        <template #default="{ row }">¥{{ (row.memberPrice || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="pv" label="PV值" width="80" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="row._statusLoading"
            @change="(val: any) => handleToggleStatus(row, val as boolean)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button
            type="warning"
            link
            size="small"
            @click="handleToggleShelf(row)"
          >
            <el-icon><SwitchButton /></el-icon>{{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="ds-empty">
          <div class="ds-empty__icon">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="ds-empty__text">暂无产品数据</div>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑产品' : '添加产品'"
      width="600px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categoryList"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" placeholder="请输入副标题" />
        </el-form-item>
        <el-form-item label="零售价" prop="retailPrice">
          <el-input-number
            v-model="form.retailPrice"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="会员价" prop="memberPrice">
          <el-input-number
            v-model="form.memberPrice"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="PV值" prop="pv">
          <el-input-number v-model="form.pv" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产品图片" prop="mainImage">
          <ImageUpload v-model="form.mainImage" module="product" />
        </el-form-item>
        <el-form-item label="产品详情" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="3"
            placeholder="请输入产品详情"
          />
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
import { Goods, Plus, Search, Refresh, Edit, SwitchButton } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'product:manage'],
})

const { get, post, put } = useApi()

const loading = ref(false)
const tableData = ref<any[]>([])
const categoryList = ref<any[]>([])

const searchParams = reactive({
  keyword: '',
  categoryId: '' as string | number,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = {
  id: null as number | null,
  name: '',
  categoryId: '' as string | number,
  subtitle: '',
  retailPrice: 0,
  memberPrice: 0,
  pv: 0,
  stock: 0,
  mainImage: '',
  detail: '',
}

const form = reactive({ ...defaultForm })

const rules: FormRules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择产品分类', trigger: 'change' }],
  retailPrice: [{ required: true, message: '请输入零售价', trigger: 'blur' }],
  memberPrice: [{ required: true, message: '请输入会员价', trigger: 'blur' }],
  pv: [{ required: true, message: '请输入PV值', trigger: 'blur' }],
}

async function fetchCategories() {
  try {
    const res: any = await get('/api/product/category/tree')
    categoryList.value = res.data || res || []
  } catch {
    categoryList.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await get('/api/product/page', {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchParams.keyword || undefined,
      categoryId: searchParams.categoryId || undefined,
    })
    const data = res.data || res
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取产品列表失败')
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
  searchParams.categoryId = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    categoryId: row.categoryId || '',
    subtitle: row.subtitle || '',
    retailPrice: row.retailPrice || 0,
    memberPrice: row.memberPrice || 0,
    pv: row.pv || 0,
    stock: row.stock || 0,
    mainImage: row.mainImage || '',
    detail: row.detail || '',
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, defaultForm)
  formRef.value?.resetFields()
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value && form.id) {
      await put('/api/product', payload)
      ElMessage.success('更新成功')
    } else {
      await post('/api/product', payload)
      ElMessage.success('新增成功')
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
    await put(`/api/product/${row.id}/status`, undefined, { params: { status: val ? 1 : 0 } })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已上架' : '已下架')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    row._statusLoading = false
  }
}

function handleToggleShelf(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  handleToggleStatus(row, newStatus === 1)
}

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>