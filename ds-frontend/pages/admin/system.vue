<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Setting /></el-icon>
        <span>系统管理</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="user">
        <div class="ds-table-toolbar">
          <div class="ds-table-toolbar__left">
            <el-input
              v-model="userParams.keyword"
              placeholder="用户名/姓名"
              clearable
              style="width: 200px;"
              @clear="fetchUsers"
              @keyup.enter="fetchUsers"
            />
            <el-button type="primary" @click="fetchUsers">
              <el-icon><Search /></el-icon>搜索
            </el-button>
          </div>
          <div class="ds-table-toolbar__right">
            <el-button type="primary" @click="handleAddUser">
              <el-icon><Plus /></el-icon>新增用户
            </el-button>
          </div>
        </div>

        <el-table v-loading="userLoading" :data="userList" stripe>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <span :class="row.status === 1 ? 'ds-status-tag--active' : 'ds-status-tag--disabled'">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditUser(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click="handleToggleUserStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="ds-empty">
              <div class="ds-empty__icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="ds-empty__text">暂无用户数据</div>
            </div>
          </template>
        </el-table>

        <el-pagination
          v-model:current-page="userPagination.page"
          v-model:page-size="userPagination.pageSize"
          :total="userPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </el-tab-pane>

      <!-- 角色管理 -->
      <el-tab-pane label="角色管理" name="role">
        <div class="ds-table-toolbar">
          <div class="ds-table-toolbar__left"></div>
          <div class="ds-table-toolbar__right">
            <el-button type="primary" @click="handleAddRole">
              <el-icon><Plus /></el-icon>新增角色
            </el-button>
          </div>
        </div>

        <el-table v-loading="roleLoading" :data="roleList" stripe>
          <el-table-column prop="name" label="角色名称" width="150" />
          <el-table-column prop="roleCode" label="编码" width="150" />
          <el-table-column prop="remark" label="备注" min-width="200">
            <template #default="{ row }">{{ row.remark || row.description || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditRole(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDeleteRole(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="ds-empty">
              <div class="ds-empty__icon">
                <el-icon><Tickets /></el-icon>
              </div>
              <div class="ds-empty__text">暂无角色数据</div>
            </div>
          </template>
        </el-table>
      </el-tab-pane>

      <!-- 参数配置 -->
      <el-tab-pane label="参数配置" name="config">
        <el-table v-loading="configLoading" :data="configList" stripe>
          <el-table-column prop="configKey" label="配置键" width="220" />
          <el-table-column prop="configValue" label="配置值" min-width="220" />
          <el-table-column prop="remark" label="备注" width="200">
            <template #default="{ row }">{{ row.remark || row.description || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditConfig(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="ds-empty">
              <div class="ds-empty__icon">
                <el-icon><Setting /></el-icon>
              </div>
              <div class="ds-empty__text">暂无配置数据</div>
            </div>
          </template>
        </el-table>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="log">
        <div class="ds-search-bar">
          <el-input
            v-model="logParams.keyword"
            placeholder="操作人/操作内容"
            clearable
            @clear="fetchLogs"
          />
          <el-date-picker
            v-model="logParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleLogSearch"
          />
          <el-button type="primary" @click="handleLogSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
        </div>

        <el-table v-loading="logLoading" :data="logList" stripe>
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="action" label="操作" width="120" />
          <el-table-column prop="description" label="描述" min-width="200">
            <template #default="{ row }">{{ row.description || row.detail || '-' }}</template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="createTime" label="时间" width="170" />
          <template #empty>
            <div class="ds-empty">
              <div class="ds-empty__icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="ds-empty__text">暂无操作日志</div>
            </div>
          </template>
        </el-table>

        <el-pagination
          v-model:current-page="logPagination.page"
          v-model:page-size="logPagination.pageSize"
          :total="logPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="userEditing ? '编辑用户' : '新增用户'"
      width="500px"
      @close="resetUserForm"
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="userEditing" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="!userEditing" label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="r in roleList"
              :key="r.id"
              :label="r.name"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSubmitting" @click="submitUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色表单对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      :title="roleEditing ? '编辑角色' : '新增角色'"
      width="500px"
      @close="resetRoleForm"
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="请输入角色编码" :disabled="roleEditing" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" :rows="3" placeholder="角色备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 配置编辑对话框 -->
    <el-dialog v-model="configDialogVisible" title="编辑配置" width="500px" @close="resetConfigForm">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="100px">
        <el-form-item label="配置键">
          <el-input v-model="configForm.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="configForm.configValue" type="textarea" :rows="3" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="configForm.remark" type="textarea" :rows="2" placeholder="配置备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSubmitting" @click="submitConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Setting, Search, Plus, Edit, Delete, User, Tickets, Document } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage'],
})

const { get, post, put, del } = useApi()

const activeTab = ref('user')

// --- 用户管理 ---
const userLoading = ref(false)
const userList = ref<any[]>([])

const userParams = reactive({ keyword: '' })

const userPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const userDialogVisible = ref(false)
const userEditing = ref(false)
const userSubmitting = ref(false)
const userFormRef = ref<FormInstance>()

const defaultUserForm = {
  id: null as number | null,
  username: '',
  realName: '',
  password: '',
  phone: '',
  roleIds: [] as number[],
}

const userForm = reactive({ ...defaultUserForm })

const userRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (!userEditing.value && !value) {
          callback(new Error('请输入密码'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// --- 角色管理 ---
const roleLoading = ref(false)
const roleList = ref<any[]>([])

const roleDialogVisible = ref(false)
const roleEditing = ref(false)
const roleSubmitting = ref(false)
const roleFormRef = ref<FormInstance>()

const defaultRoleForm = {
  id: null as number | null,
  name: '',
  roleCode: '',
  remark: '',
}

const roleForm = reactive({ ...defaultRoleForm })

const roleRules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

// --- 参数配置 ---
const configLoading = ref(false)
const configList = ref<any[]>([])

const configDialogVisible = ref(false)
const configSubmitting = ref(false)
const configFormRef = ref<FormInstance>()
const configForm = reactive({
  id: null as number | null,
  configKey: '',
  configValue: '',
  remark: '',
})

const configRules: FormRules = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

// --- 操作日志 ---
const logLoading = ref(false)
const logList = ref<any[]>([])

const logParams = reactive({
  keyword: '',
  dateRange: null as string[] | null,
})

const logPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

// --- 用户方法 ---
async function fetchUsers() {
  userLoading.value = true
  try {
    const res: any = await get('/api/system/user/page', {
      page: userPagination.page,
      size: userPagination.pageSize,
      keyword: userParams.keyword || undefined,
    })
    const data = res.data || res
    userList.value = data.records || data.list || []
    userPagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取用户列表失败')
  } finally {
    userLoading.value = false
  }
}

function handleAddUser() {
  userEditing.value = false
  resetUserForm()
  userDialogVisible.value = true
}

function handleEditUser(row: any) {
  userEditing.value = true
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    password: '',
    roleIds: row.roleIds || [],
  })
  userDialogVisible.value = true
}

function resetUserForm() {
  Object.assign(userForm, defaultUserForm)
  userFormRef.value?.resetFields()
}

async function submitUser() {
  if (!userFormRef.value) return
  const valid = await userFormRef.value.validate().catch(() => false)
  if (!valid) return

  userSubmitting.value = true
  try {
    const payload: any = {
      username: userForm.username,
      realName: userForm.realName,
      phone: userForm.phone,
      roleIds: userForm.roleIds,
    }
    if (userEditing.value) {
      payload.id = userForm.id
      await put('/api/system/user', payload)
      ElMessage.success('更新成功')
    } else {
      payload.password = userForm.password
      await post('/api/system/user', payload)
      ElMessage.success('新增成功')
    }
    userDialogVisible.value = false
    fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    userSubmitting.value = false
  }
}

async function handleToggleUserStatus(row: any) {
  const action = row.status === 1 ? '禁用' : '启用'
  ElMessageBox.confirm(`确定要${action}用户「${row.username}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await put(`/api/system/user/${row.id}/status`, undefined, { params: { status: row.status === 1 ? 0 : 1 } })
      ElMessage.success(`${action}成功`)
      fetchUsers()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    }
  }).catch(() => {})
}

// --- 角色方法 ---
async function fetchRoles() {
  roleLoading.value = true
  try {
    const res: any = await get('/api/system/role/list')
    roleList.value = res.data || res || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色列表失败')
  } finally {
    roleLoading.value = false
  }
}

function handleAddRole() {
  roleEditing.value = false
  resetRoleForm()
  roleDialogVisible.value = true
}

function handleEditRole(row: any) {
  roleEditing.value = true
  Object.assign(roleForm, {
    id: row.id,
    name: row.name,
    roleCode: row.roleCode,
    remark: row.remark || row.description || '',
  })
  roleDialogVisible.value = true
}

function resetRoleForm() {
  Object.assign(roleForm, defaultRoleForm)
  roleFormRef.value?.resetFields()
}

async function submitRole() {
  if (!roleFormRef.value) return
  const valid = await roleFormRef.value.validate().catch(() => false)
  if (!valid) return

  roleSubmitting.value = true
  try {
    const payload: any = {
      name: roleForm.name,
      roleCode: roleForm.roleCode,
      remark: roleForm.remark,
    }
    if (roleEditing.value) {
      payload.id = roleForm.id
      await put('/api/system/role', payload)
      ElMessage.success('更新成功')
    } else {
      await post('/api/system/role', payload)
      ElMessage.success('新增成功')
    }
    roleDialogVisible.value = false
    fetchRoles()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    roleSubmitting.value = false
  }
}

async function handleDeleteRole(row: any) {
  ElMessageBox.confirm(`确定要删除角色「${row.name}」吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await del(`/api/system/role/${row.id}`)
      ElMessage.success('删除成功')
      fetchRoles()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// --- 配置方法 ---
async function fetchConfigs() {
  configLoading.value = true
  try {
    const res: any = await get('/api/system/config/list')
    configList.value = res.data || res || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取配置列表失败')
  } finally {
    configLoading.value = false
  }
}

function handleEditConfig(row: any) {
  Object.assign(configForm, {
    id: row.id,
    configKey: row.configKey,
    configValue: row.configValue,
    remark: row.remark || row.description || '',
  })
  configDialogVisible.value = true
}

function resetConfigForm() {
  configForm.id = null
  configForm.configKey = ''
  configForm.configValue = ''
  configForm.remark = ''
  configFormRef.value?.resetFields()
}

async function submitConfig() {
  if (!configFormRef.value) return
  const valid = await configFormRef.value.validate().catch(() => false)
  if (!valid) return

  configSubmitting.value = true
  try {
    await put(`/api/system/config/${configForm.configKey}?value=${encodeURIComponent(configForm.configValue)}`)
    ElMessage.success('更新成功')
    configDialogVisible.value = false
    fetchConfigs()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    configSubmitting.value = false
  }
}

// --- 日志方法 ---
async function fetchLogs() {
  logLoading.value = true
  try {
    const params: any = {
      page: logPagination.page,
      size: logPagination.pageSize,
      keyword: logParams.keyword || undefined,
    }
    if (logParams.dateRange && logParams.dateRange.length === 2) {
      params.startDate = logParams.dateRange[0]
      params.endDate = logParams.dateRange[1]
    }
    const res: any = await get('/api/system/log/page', params)
    const data = res.data || res
    logList.value = data.records || data.list || []
    logPagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取操作日志失败')
  } finally {
    logLoading.value = false
  }
}

function handleLogSearch() {
  logPagination.page = 1
  fetchLogs()
}

function handleTabChange(tab: string | number) {
  switch (tab) {
    case 'user':
      fetchUsers()
      break
    case 'role':
      fetchRoles()
      break
    case 'config':
      fetchConfigs()
      break
    case 'log':
      fetchLogs()
      break
  }
}

function handleSizeChange() {
  if (activeTab.value === 'user') {
    userPagination.page = 1
    fetchUsers()
  } else if (activeTab.value === 'log') {
    logPagination.page = 1
    fetchLogs()
  }
}

function handlePageChange() {
  if (activeTab.value === 'user') {
    fetchUsers()
  } else if (activeTab.value === 'log') {
    fetchLogs()
  }
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>