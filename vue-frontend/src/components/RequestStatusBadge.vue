<template>
  <span class="status-badge" :class="`status-${info.color}`">{{ info.label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { REQUEST_STATUS } from '@/utils/requestStatus.js'

const props = defineProps({
  status: { type: String, required: true }
})

const info = computed(() => REQUEST_STATUS[props.status] || { label: props.status, color: 'gray' })
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
/* 상태 흐름 배지: 대기=중립, 접수=브랜드 강조, 진행중=주의, 완료=성공, 반려·취소=위험/중립 */
.status-blue   { background: var(--color-bg-tertiary); color: var(--color-text-secondary); }
.status-indigo { background: var(--color-primary-light); color: var(--color-primary-dark); }
.status-orange { background: var(--color-warning-light); color: var(--color-warning); }
.status-green  { background: var(--color-success-light); color: var(--color-success); }
.status-red    { background: var(--color-danger-light); color: var(--color-danger); }
.status-gray   { background: var(--color-bg-tertiary); color: var(--color-text-muted); }
</style>
