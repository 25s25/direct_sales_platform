<template>
  <OssUpload
    :model-value="modelValue"
    :module="module"
    :biz-id="bizId"
    type="image"
    :max-size="5"
    @update:model-value="(val: string) => $emit('update:modelValue', val)"
  />
</template>

<script setup lang="ts">
defineProps<{
  modelValue?: string | number
  module?: string
  bizId?: string | number
}>()

defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()
</script>
