<template>
  <router-link :to="`/resources/${course.id}`" class="course-card">
    <!-- 썸네일 -->
    <div class="card-thumb" :class="style.bg">
      <span class="thumb-icon">{{ style.icon }}</span>
    </div>

    <!-- 내용 -->
    <div class="card-body">
      <div class="card-top">
        <span class="badge" :class="style.badge">{{ course.category }}</span>
        <span class="badge" :class="course.status === 'INACTIVE' ? 'badge-neutral' : 'badge-success'">
          {{ course.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE' }}
        </span>
      </div>
      <h3 class="card-title">{{ course.title }}</h3>
      <p class="card-desc">{{ course.description || '설명이 등록되지 않았습니다.' }}</p>
      <div class="card-footer">
        <span class="provision-count">제공 완료 {{ (course.enrollmentCount ?? 0).toLocaleString() }}회</span>
      </div>
      <span class="detail-link">상세보기 →</span>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { useCourseStore } from '@/store/course.js'

const props = defineProps({
  course: { type: Object, required: true }
})

const courseStore = useCourseStore()
const style = computed(() => courseStore.getCategoryStyle(props.course))
</script>

<style scoped>
.course-card {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: var(--transition);
  cursor: pointer;
}
.course-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-hover);
}
.card-thumb {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
/* 카테고리는 아이콘으로 구분하므로 썸네일 배경은 중립 톤으로 통일한다 */
.thumb-teal,
.thumb-blue,
.thumb-amber,
.thumb-purple,
.thumb-pink,
.thumb-gray { background: var(--color-bg-tertiary); }
.thumb-icon {
  font-size: 36px;
}
.card-body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.4;
}
.card-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.6em;
}
.card-footer {
  display: flex;
  align-items: center;
  margin-top: 2px;
}
.provision-count {
  font-size: 11px;
  color: var(--color-text-muted);
}
.detail-link {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
}
</style>
