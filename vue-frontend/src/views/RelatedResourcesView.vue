<template>
  <div class="page-wrapper">
    <AppHeader />
    <div class="page-layout">
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-label">메뉴</div>

          <router-link to="/resources" class="sidebar-item">
            <span class="si-icon">🖥️</span> 리소스 목록
          </router-link>

          <router-link to="/requests/my" class="sidebar-item">
            <span class="si-icon">✅</span> 내 신청 내역
          </router-link>

          <router-link to="/related" class="sidebar-item active">
            <span class="si-icon">🔗</span> 연관 리소스
          </router-link>

          <router-link to="/mypage" class="sidebar-item">
            <span class="si-icon">⭐</span> 마이페이지
          </router-link>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-label">계정</div>
          <button class="sidebar-item sidebar-btn" @click="handleLogout">
            <span class="si-icon">🚪</span> 로그아웃
          </button>
        </div>
      </aside>

      <main class="main-content">
        <h1 class="page-title">연관 리소스</h1>
        <p class="page-desc">
          <template v-if="basedOnCategories.length">
            보유하신 <strong>{{ basedOnCategories.join(', ') }}</strong> 카테고리 리소스를 기반으로 추천합니다.
          </template>
          <template v-else>
            아직 제공받은 리소스가 없어 추천할 항목이 없습니다.
          </template>
        </p>

        <div v-if="loading" class="loading-center">
          <div class="spinner"></div>
        </div>

        <div v-else-if="resources.length" class="related-grid">
          <CourseCard v-for="r in resources" :key="r.id" :course="r" />
        </div>

        <div v-else class="empty-state">
          <p class="empty-icon">🔗</p>
          <p>아직 안내할 연관 리소스가 없습니다.</p>
          <router-link to="/resources" class="btn btn-primary" style="margin-top:16px;">
            리소스 둘러보기
          </router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import CourseCard from '@/components/CourseCard.vue'
import { relatedResourceApi } from '@/api/recommend.js'
import { useAuthStore } from '@/store/auth.js'
import { useCourseStore } from '@/store/course.js'

const router = useRouter()
const auth = useAuthStore()
const courseStore = useCourseStore()

const resources = ref([])
const basedOnCategories = ref([])
const loading = ref(true)

function handleLogout() {
  auth.logout()
  router.push('/')
}

async function loadRelatedResources() {
  loading.value = true
  try {
    const res = await relatedResourceApi.getMine()
    // GET /api/recommend/me 응답: { userId, basedOnCategories, relatedCategories, resources: [...] }
    const list = Array.isArray(res.data?.resources)
      ? res.data.resources
      : Array.isArray(res.data?.data)
        ? res.data.data
        : Array.isArray(res.data)
          ? res.data
          : []
    resources.value = list.map(courseStore.normalizeCourse)
    basedOnCategories.value = Array.isArray(res.data?.basedOnCategories)
      ? res.data.basedOnCategories.map(courseStore.normalizeCategory)
      : []
  } catch (e) {
    console.error('[RelatedResources] failed to load:', e)
    resources.value = []
    basedOnCategories.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadRelatedResources)
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  background: var(--color-bg-secondary);
}

.page-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 28px;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 8px;
}

.sidebar-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
  padding: 8px 12px 4px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: var(--transition);
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  cursor: pointer;
  font-family: var(--font-sans);
  text-decoration: none;
}

.sidebar-item:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.sidebar-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 500;
}

.si-icon {
  font-size: 15px;
}

.main-content {
  min-width: 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 992px) {
  .page-layout {
    grid-template-columns: 1fr;
  }

  .related-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
