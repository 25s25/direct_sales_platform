<template>
  <div class="ds-oss-upload">
    <el-upload
      :action="uploadAction"
      :data="uploadData"
      :headers="uploadHeaders"
      :accept="accept"
      :show-file-list="type === 'file'"
      :limit="1"
      :before-upload="handleBeforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-remove="handleRemove"
      :file-list="fileList"
      class="ds-oss-upload__uploader"
    >
      <template v-if="type === 'image'">
        <div v-if="resolvedPreviewUrl" class="ds-oss-upload__image-preview">
          <el-image :src="resolvedPreviewUrl" fit="cover" class="ds-oss-upload__image" />
          <div class="ds-oss-upload__image-actions">
            <el-icon @click.stop="previewVisible = true"><View /></el-icon>
            <el-icon @click.stop="handleClear"><Delete /></el-icon>
          </div>
        </div>
        <div v-else class="ds-oss-upload__placeholder">
          <el-icon><Plus /></el-icon>
          <span>点击上传</span>
        </div>
      </template>
      <template v-else>
        <el-button type="primary" :icon="Upload">上传文件</el-button>
      </template>
    </el-upload>

    <el-image-viewer
      v-if="previewVisible && resolvedPreviewUrl"
      :url-list="[resolvedPreviewUrl]"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Plus, View, Delete, Upload } from '@element-plus/icons-vue'

const { get } = useApi()

const props = defineProps<{
  modelValue?: string | number
  module?: string
  bizId?: string | number
  accept?: string
  maxSize?: number
  type?: 'file' | 'image'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const type = computed(() => props.type || 'file')
const module = computed(() => props.module || 'common')
const bizId = computed(() => props.bizId || '')
const accept = computed(() => props.accept || (type.value === 'image' ? 'image/*' : ''))

const uploadAction = '/api/oss/upload'
const uploadHeaders = computed(() => {
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : ''
  return token ? { token } : {}
})
const uploadData = computed(() => {
  const data: Record<string, string> = { module: module.value }
  if (bizId.value) {
    data.bizId = String(bizId.value)
  }
  return data
})

const fileList = ref<any[]>([])
const previewVisible = ref(false)
const resolvedPreviewUrl = ref('')

watch(
  () => props.modelValue,
  async (value) => {
    resolvedPreviewUrl.value = ''
    if (!value) {
      fileList.value = []
      return
    }
    if (typeof value === 'string' && value.startsWith('http')) {
      resolvedPreviewUrl.value = value
      fileList.value = [{ name: '已上传文件', url: value }]
      return
    }
    try {
      const res: any = await get(`/api/oss/${value}/url`)
      const url = res?.data || res || ''
      if (url) {
        resolvedPreviewUrl.value = url
        fileList.value = [{ name: '已上传文件', url }]
      } else {
        fileList.value = []
      }
    } catch {
      fileList.value = []
    }
  },
  { immediate: true }
)

function handleBeforeUpload(file: File) {
  if (props.maxSize && file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  if (type.value === 'image' && !file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return false
  }
  return true
}

function handleSuccess(response: any) {
  const url = response?.data?.accessUrl || response?.data?.url || response?.data || ''
  if (url) {
    emit('update:modelValue', url)
    if (typeof url === 'string' && url.startsWith('http')) {
      fileList.value = [{ name: '已上传文件', url }]
    } else {
      fileList.value = []
    }
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败，未返回文件地址')
  }
}

function handleError(err: any) {
  const message = err?.message || '上传失败'
  ElMessage.error(message)
}

function handleRemove() {
  emit('update:modelValue', '')
  fileList.value = []
}

function handleClear() {
  emit('update:modelValue', '')
  fileList.value = []
}
</script>

<style scoped lang="scss">
.ds-oss-upload {
  &__uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color 0.25s ease;
      &:hover { border-color: #409eff; }
    }
  }

  &__placeholder {
    width: 120px;
    height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8c939d;
    .el-icon { font-size: 28px; margin-bottom: 8px; }
    span { font-size: 12px; }
  }

  &__image-preview {
    width: 120px;
    height: 120px;
    position: relative;
  }

  &__image {
    width: 100%;
    height: 100%;
    border-radius: 6px;
  }

  &__image-actions {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    opacity: 0;
    transition: opacity 0.25s ease;
    color: #fff;
    font-size: 18px;
    .el-icon { cursor: pointer; }
    &:hover { opacity: 1; }
  }
}
</style>
