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

          <router-link to="/requests/my" class="sidebar-item active">
            <span class="si-icon">✅</span> 내 신청 내역
          </router-link>

          <router-link to="/related" class="sidebar-item">
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
        <h1 class="page-title">내 신청 내역</h1>

        <div v-if="loading" class="loading-center">
          <div class="spinner"></div>
        </div>

        <div v-else-if="error" class="empty-state">
          <p>{{ error }}</p>
          <button class="btn btn-ghost" style="margin-top:16px;" @click="loadRequests">다시 시도</button>
        </div>

        <div v-else-if="requests.length" class="enrollment-list fade-in">
          <div v-for="item in requests" :key="item.id" class="enrollment-card">
            <div class="enroll-thumb" :class="getStyle(item).bg">
              <span class="thumb-icon">{{ getStyle(item).icon }}</span>
            </div>

            <div class="enroll-info">
              <span class="badge" :class="getStyle(item).badge">
                {{ getResourceCategory(item) }}
              </span>
              <h3 class="enroll-title">{{ getResourceName(item) }}</h3>
              <p class="enroll-reason">신청 사유: {{ item.reason || '-' }}</p>
              <p class="enroll-sub">
                수량 {{ item.quantity ?? '-' }}개 · 희망 제공일 {{ item.desiredDate || '-' }} · 신청일 {{ formatDate(item.createdAt) }}
              </p>
              <p v-if="item.rejectReason" class="enroll-reject">반려 사유: {{ item.rejectReason }}</p>
              <p v-if="item.cancelReason" class="enroll-reject">취소 사유: {{ item.cancelReason }}</p>
            </div>

            <div class="enroll-status">
              <RequestStatusBadge :status="item.status" />
              <div class="enroll-status-actions">
                <router-link :to="`/requests/${item.id}`" class="btn btn-ghost btn-sm">
                  상세보기
                </router-link>
                <button
                  v-if="item.status === 'PROVIDED'"
                  class="btn btn-ghost btn-sm"
                  :disabled="returningId === item.id"
                  @click="handleReturn(item)"
                >
                  반납
                </button>
                <button
                  v-if="['REQUESTED', 'ACCEPTED', 'PROVISIONING'].includes(item.status)"
                  class="btn btn-ghost btn-sm btn-danger"
                  @click="openCancelModal(item)"
                >
                  취소
                </button>
                <button
                  v-if="['CANCELLED', 'RETURNED'].includes(item.status)"
                  class="btn btn-ghost btn-sm btn-danger"
                  :disabled="deletingId === item.id"
                  @click="handleDelete(item)"
                >
                  삭제
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p class="empty-icon">📭</p>
          <p>신청한 리소스가 없습니다.</p>
          <router-link to="/resources" class="btn btn-primary" style="margin-top:16px;">
            리소스 둘러보기
          </router-link>
        </div>
      </main>
    </div>

    <ReasonModal
      v-if="cancelTarget"
      title="신청 취소"
      description="취소 사유를 입력해 주세요."
      :submitting="cancelling"
      @close="cancelTarget = null"
      @confirm="handleCancelConfirm"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import RequestStatusBadge from '@/components/RequestStatusBadge.vue'
import ReasonModal from '@/components/ReasonModal.vue'
import { requestApi } from '@/api/enrollment.js'
import { useAuthStore } from '@/store/auth.js'
import { useCourseStore } from '@/store/course.js'
import { mapErrorMessage } from '@/utils/errorMessage.js'

const router = useRouter()
const auth = useAuthStore()
const courseStore = useCourseStore()

const requests = ref([])
const loading = ref(true)
const error = ref('')

const cancelTarget = ref(null)
const cancelling = ref(false)
const returningId = ref(null)
const deletingId = ref(null)

function getResourceName(item) {
  return item.course?.title ?? item.resourceName ?? item.name ?? '-'
}

function getResourceCategory(item) {
  return courseStore.normalizeCategory(item.course?.category ?? item.category)
}

function getStyle(item) {
  return courseStore.getCategoryStyle({ category: getResourceCategory(item) })
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function handleLogout() {
  auth.logout()
  router.push('/')
}

function openCancelModal(item) {
  cancelTarget.value = item
}

async function handleCancelConfirm(reason) {
  if (!cancelTarget.value) return
  cancelling.value = true
  try {
    await requestApi.cancel(cancelTarget.value.id, reason)
    cancelTarget.value = null
    await loadRequests()
  } catch (e) {
    console.error('[EnrollmentView] cancel failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    cancelling.value = false
  }
}

async function handleReturn(item) {
  returningId.value = item.id
  try {
    await requestApi.return(item.id)
    await loadRequests()
  } catch (e) {
    console.error('[EnrollmentView] return failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    returningId.value = null
  }
}

async function handleDelete(item) {
  if (!window.confirm('이 신청 기록을 삭제하시겠습니까? 삭제하면 되돌릴 수 없습니다.')) return
  deletingId.value = item.id
  try {
    await requestApi.remove(item.id)
    await loadRequests()
  } catch (e) {
    console.error('[EnrollmentView] delete failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    deletingId.value = null
  }
}

async function loadRequests() {
  loading.value = true
  error.value = ''
  try {
    const res = await requestApi.getMine()
    console.log('[EnrollmentView] my requests response:', res.data)

    if (Array.isArray(res.data?.data)) {
      requests.value = res.data.data
    } else if (Array.isArray(res.data)) {
      requests.value = res.data
    } else {
      requests.value = []
    }
  } catch (e) {
    console.error('[EnrollmentView] failed to load requests:', e)
    error.value = mapErrorMessage(e)
    requests.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadRequests)
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
  margin-bottom: 24px;
}

.enrollment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.enrollment-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
  transition: var(--transition);
}

.enrollment-card:hover {
  box-shadow: var(--shadow-sm);
}

.enroll-thumb {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.thumb-icon {
  font-size: 26px;
}

.thumb-teal,
.thumb-blue,
.thumb-amber,
.thumb-purple,
.thumb-pink,
.thumb-gray { background: var(--color-bg-tertiary); }

.enroll-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.enroll-title {
  font-size: 15px;
  font-weight: 600;
}

.enroll-reason {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.enroll-sub {
  font-size: 12px;
  color: var(--color-text-muted);
}

.enroll-reject {
  font-size: 12px;
  color: var(--color-danger);
}

.enroll-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.enroll-status-actions {
  display: flex;
  gap: 6px;
}

.btn-sm {
  padding: 7px 14px;
  font-size: 13px;
}

.btn-danger {
  color: var(--color-danger);
  border-color: var(--color-danger-light);
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
}
</style>
