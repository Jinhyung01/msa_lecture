<template>
  <div class="page-wrapper">
    <AppHeader />

    <div class="page-layout">
      <router-link :to="backLink" class="back-link">← 목록으로</router-link>

      <div v-if="loading" class="loading-center">
        <div class="spinner"></div>
      </div>

      <div v-else-if="error" class="empty-state">
        <p>{{ error }}</p>
        <button class="btn btn-ghost" style="margin-top:16px;" @click="loadRequest">다시 시도</button>
      </div>

      <div v-else-if="request" class="detail-card fade-in-up">
        <div class="detail-header">
          <div>
            <span class="badge" :class="courseStore.getCategoryStyle({ category: resourceCategory }).badge">
              {{ resourceCategory }}
            </span>
            <h1 class="detail-title">{{ resourceName }}</h1>
          </div>
          <RequestStatusBadge :status="request.status" />
        </div>

        <!-- 진행 단계 -->
        <div class="progress-steps" v-if="stepInfo.step > 0">
          <div
            v-for="s in progressSteps"
            :key="s.key"
            class="progress-step"
            :class="{ active: s.step <= stepInfo.step }"
          >
            <span class="step-dot"></span>
            <span class="step-label">{{ s.label }}</span>
          </div>
        </div>

        <div class="detail-grid">
          <div class="detail-field">
            <span class="field-label">신청자</span>
            <span class="field-value">{{ requesterName }}</span>
          </div>
          <div class="detail-field">
            <span class="field-label">수량</span>
            <span class="field-value">{{ request.quantity ?? '-' }}</span>
          </div>
          <div class="detail-field">
            <span class="field-label">희망 제공일</span>
            <span class="field-value">{{ request.desiredDate || '-' }}</span>
          </div>
          <div class="detail-field">
            <span class="field-label">신청일</span>
            <span class="field-value">{{ formatDate(request.createdAt) }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h3 class="section-title">신청 사유</h3>
          <p class="section-text">{{ request.reason || '-' }}</p>
        </div>

        <div v-if="request.rejectReason" class="detail-section reason-box">
          <h3 class="section-title">반려 사유</h3>
          <p class="section-text">{{ request.rejectReason }}</p>
        </div>

        <div v-if="request.cancelReason" class="detail-section reason-box">
          <h3 class="section-title">취소 사유</h3>
          <p class="section-text">{{ request.cancelReason }}</p>
        </div>

        <div v-if="hasProvisionInfo" class="detail-section provision-box">
          <h3 class="section-title">제공 정보</h3>
          <div class="detail-grid">
            <div class="detail-field" v-if="provision.transactionId">
              <span class="field-label">작업 번호</span>
              <span class="field-value">{{ provision.transactionId }}</span>
            </div>
            <div class="detail-field" v-if="provision.resultMemo">
              <span class="field-label">제공 결과</span>
              <span class="field-value">{{ provision.resultMemo }}</span>
            </div>
            <div class="detail-field" v-if="provision.providedAt">
              <span class="field-label">제공 완료일</span>
              <span class="field-value">{{ formatDate(provision.providedAt) }}</span>
            </div>
            <div class="detail-field" v-if="provision.managerMemo">
              <span class="field-label">관리자 메모</span>
              <span class="field-value">{{ provision.managerMemo }}</span>
            </div>
            <div class="detail-field" v-if="provision.returnedAt">
              <span class="field-label">반납 완료일</span>
              <span class="field-value">{{ formatDate(provision.returnedAt) }}</span>
            </div>
          </div>
        </div>

        <div v-if="canDecide" class="detail-section decide-actions">
          <button class="btn btn-primary" :disabled="deciding" @click="handleAccept">접수</button>
          <button class="btn btn-ghost btn-danger" :disabled="deciding" @click="rejectModalOpen = true">반려</button>
        </div>
      </div>
    </div>

    <ReasonModal
      v-if="rejectModalOpen"
      title="요청 반려"
      description="반려 사유를 입력해 주세요."
      :submitting="deciding"
      @close="rejectModalOpen = false"
      @confirm="handleReject"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import RequestStatusBadge from '@/components/RequestStatusBadge.vue'
import ReasonModal from '@/components/ReasonModal.vue'
import { requestApi } from '@/api/enrollment.js'
import { provisionApi } from '@/api/provision.js'
import { useAuthStore } from '@/store/auth.js'
import { useCourseStore } from '@/store/course.js'
import { isAdmin } from '@/utils/role.js'
import { REQUEST_STATUS } from '@/utils/requestStatus.js'
import { mapErrorMessage } from '@/utils/errorMessage.js'

const route = useRoute()
const auth = useAuthStore()
const courseStore = useCourseStore()

const request = ref(null)
const provision = ref(null)
const loading = ref(true)
const error = ref('')
const deciding = ref(false)
const rejectModalOpen = ref(false)

const canDecide = computed(() => isAdmin(auth.user) && request.value?.status === 'REQUESTED')

const backLink = computed(() => (isAdmin(auth.user) ? '/admin/requests' : '/requests/my'))

const resourceName = computed(() => request.value?.course?.title ?? request.value?.resourceName ?? request.value?.name ?? '-')
const resourceCategory = computed(() => courseStore.normalizeCategory(request.value?.course?.category ?? request.value?.category))
const requesterName = computed(() => request.value?.userName ?? request.value?.requesterName ?? request.value?.requester?.name ?? '-')

const stepInfo = computed(() => REQUEST_STATUS[request.value?.status] || { step: 0 })

const progressSteps = [
  { key: 'REQUESTED', label: '신청 완료', step: 1 },
  { key: 'ACCEPTED', label: '관리자 접수', step: 2 },
  { key: 'PROVISIONING', label: '제공 작업 중', step: 3 },
  { key: 'PROVIDED', label: '제공 완료', step: 4 },
  { key: 'RETURN_REQUESTED', label: '반납 신청 완료', step: 5 },
  { key: 'RETURNED', label: '반납 완료', step: 6 }
]

const hasProvisionInfo = computed(() =>
  !!(provision.value?.transactionId || provision.value?.resultMemo || provision.value?.managerMemo)
)

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

async function loadProvision(paymentId) {
  if (!paymentId) {
    provision.value = null
    return
  }
  try {
    const res = await provisionApi.getById(paymentId)
    provision.value = res.data?.data ?? res.data ?? null
  } catch (e) {
    console.error('[RequestDetail] failed to load provision:', e)
    provision.value = null
  }
}

async function loadRequest() {
  loading.value = true
  error.value = ''
  try {
    const res = await requestApi.getById(route.params.id)
    console.log('[RequestDetail] response:', res.data)
    request.value = res.data?.data ?? res.data ?? null
    await loadProvision(request.value?.paymentId)
  } catch (e) {
    console.error('[RequestDetail] failed to load request:', e)
    error.value = mapErrorMessage(e)
    request.value = null
  } finally {
    loading.value = false
  }
}

async function handleAccept() {
  if (!request.value?.paymentId) return
  deciding.value = true
  try {
    await provisionApi.accept(request.value.paymentId)
    await loadRequest()
  } catch (e) {
    console.error('[RequestDetail] accept failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    deciding.value = false
  }
}

async function handleReject(reason) {
  if (!request.value?.paymentId) return
  deciding.value = true
  try {
    await provisionApi.reject(request.value.paymentId, reason)
    rejectModalOpen.value = false
    await loadRequest()
  } catch (e) {
    console.error('[RequestDetail] reject failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    deciding.value = false
  }
}

onMounted(loadRequest)
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  background: var(--color-bg-secondary);
}

.page-layout {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 24px;
}

.back-link {
  display: inline-block;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
  transition: var(--transition);
}

.back-link:hover {
  color: var(--color-primary);
}

.detail-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-sm);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-title {
  font-size: 22px;
  font-weight: 700;
  margin-top: 8px;
}

.progress-steps {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.progress-step {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  opacity: 0.4;
}

.progress-step.active {
  opacity: 1;
}

/* 이전 점과 현재 점을 잇는 연결선. 현재 점이 완료 상태(active)면 선명한 실선,
   아직 완료되지 않았으면 흐린 점선으로 그린다. */
.progress-step:not(:first-child)::before {
  content: '';
  position: absolute;
  top: 4px;
  left: -50%;
  width: 100%;
  height: 0;
  border-top: 2px dashed var(--color-primary);
  z-index: 0;
}

.progress-step.active:not(:first-child)::before {
  border-top-style: solid;
}

.step-dot {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-primary);
}

.step-label {
  font-size: 11px;
  color: var(--color-text-secondary);
  text-align: center;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

.field-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.field-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-section {
  margin-bottom: 18px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.section-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  white-space: pre-line;
}

.reason-box .section-text {
  color: var(--color-danger);
}

.provision-box {
  background: var(--color-success-light);
  border-radius: var(--radius-md);
  padding: 16px;
}

.provision-box .detail-field {
  background: var(--color-bg-primary);
}

.decide-actions {
  display: flex;
  gap: 10px;
  padding-top: 6px;
  border-top: 1px solid var(--color-border);
}

.decide-actions .btn-danger {
  color: var(--color-danger);
  border-color: var(--color-danger-light);
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--color-text-muted);
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

@media (max-width: 640px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
