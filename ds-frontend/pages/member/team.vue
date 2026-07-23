<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><User /></el-icon>
        <span>我的团队</span>
      </div>
      <div class="ds-page__header-actions">
        <el-button @click="expandAll">
          <el-icon><Plus /></el-icon>展开全部
        </el-button>
        <el-button @click="collapseAll">
          <el-icon><Minus /></el-icon>收起全部
        </el-button>
      </div>
    </div>

    <div class="ds-search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索成员姓名或手机号"
        clearable
        @clear="filterTree"
        @keyup.enter="filterTree"
      />
      <el-button type="primary" @click="filterTree">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetSearch">
        <el-icon><Refresh /></el-icon>重置
      </el-button>
    </div>

    <div v-loading="loading" class="ds-team-tree">
      <el-tree
        v-if="memberStore.teamTree.length > 0"
        ref="treeRef"
        :data="memberStore.teamTree"
        :props="treeProps"
        node-key="id"
        :default-expand-all="false"
        :filter-node-method="filterNode"
        highlight-current
      >
        <template #default="{ node, data }">
          <div style="display: flex; align-items: center; gap: 10px; width: 100%;">
            <el-avatar :size="28">
              {{ data.realName?.charAt(0) || '会' }}
            </el-avatar>
            <div style="display: flex; align-items: center; gap: 6px;">
              <span style="font-weight: 500; font-size: 14px;">{{ data.realName || data.phone }}</span>
              <el-tag :type="levelTagType(data.levelName) as any" size="small">
                {{ data.levelName || '-' }}
              </el-tag>
            </div>
            <div style="margin-left: auto; font-size: 12px; color: #909399; display: flex; gap: 12px;">
              <span v-if="data.memberNo">编号: {{ data.memberNo }}</span>
              <span v-if="data.phone">{{ data.phone }}</span>
            </div>
          </div>
        </template>
      </el-tree>
      <div v-else-if="!loading" class="ds-empty">
        <div class="ds-empty__icon">
          <el-icon><User /></el-icon>
        </div>
        <div class="ds-empty__text">暂无团队数据</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Plus, Search, Refresh, Minus } from '@element-plus/icons-vue'
import { useMemberStore } from '~/stores/member'
import type { ElTree } from 'element-plus'

definePageMeta({
  layout: 'member',
  middleware: 'auth',
})

const authStore = useAuthStore()
const memberStore = useMemberStore()

const loading = ref(false)
const searchKeyword = ref('')
const treeRef = ref<InstanceType<typeof ElTree>>()

const treeProps = {
  label: 'realName',
  children: 'children',
}

function levelLabel(levelName: string | undefined) {
  return levelName || '-'
}

function levelTagType(levelName: string | undefined): any {
  const map: Record<string, string> = {
    '普通会员': 'info',
    '银卡会员': '',
    '金卡会员': 'warning',
    '钻石会员': 'danger',
  }
  return map[levelName as string] || 'info'
}

function filterNode(value: string, data: any): boolean {
  if (!value) return true
  const keyword = value.toLowerCase()
  return (
    (data.realName && data.realName.toLowerCase().includes(keyword)) ||
    (data.phone && data.phone.includes(keyword))
  )
}

function filterTree() {
  treeRef.value?.filter(searchKeyword.value)
}

function resetSearch() {
  searchKeyword.value = ''
  treeRef.value?.filter('')
}

function expandAll() {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.keys(nodes).forEach((key) => {
    nodes[key].expanded = true
  })
}

function collapseAll() {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.keys(nodes).forEach((key) => {
    nodes[key].expanded = false
  })
}

async function fetchTeamTree() {
  loading.value = true
  try {
    const memberId = authStore.user?.id || undefined
    await memberStore.fetchTeamTree(memberId)
  } catch (error: any) {
    ElMessage.error(error.message || '获取团队数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTeamTree()
})
</script>