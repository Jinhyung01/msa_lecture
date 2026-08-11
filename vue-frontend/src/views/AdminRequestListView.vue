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

          <router-link to="/resources/new" class="sidebar-item">
            <span class="si-icon">✍️</span> 리소스 등록
          </router-link>

          <router-link to="/admin/requests" class="sidebar-item active">
            <span class="si-icon">🗂️</span> 요청 관리
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
        <h1 class="page-title">리소스 요청 관리</h1>

        <!-- 상태별 건수 카드 -->
        <div class="count-cards">
          <div class="count-card">
            <div class="count-label">요청 대기</div>
            <div class="count-value">{{ counts.requested }}</div>
          </div>
          <div class="count-card">
            <div class="count-label">제공 완료</div>
            <div class="count-value">{{ counts.provided }}</div>
          </div>
          <div class="count-card">
            <div class="count-label">수거 완료</div>
            <div class="count-value">{{ counts.returned }}</div>
          </div>
          <div class="count-card">
            <div class="count-label">취소/반려</div>
            <div class="count-value">{{ counts.cancelledOrRejected }}</div>
          </div>
        </div>

        <!-- 필터 -->
        <div class="filter-bar">
          <select v-model="statusFilter" class="filter-select">
            <option value="ALL">전체 상태</option>
            <option v-for="s in statusOptions" :key="s" :value="s">{{ REQUEST_STATUS[s].label }}</option>
          </select>
          <select v-model="categoryFilter" class="filter-select">
            <option value="전체">전체 카테고리</option>
            <option v-for="c in courseStore.categories.filter(c => c !== '전체')" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="searchText" type="text" class="filter-search" placeholder="신청자 또는 리소스명 검색" />
        </div>

        <div v-if="loading" class="loading-center">
          <div class="spinner"></div>
        </div>

        <div v-else-if="error" class="empty-state">
          <p>{{ error }}</p>
          <button class="btn btn-ghost" style="margin-top:16px;" @click="loadRequests">다시 시도</button>
        </div>

        <div v-else-if="filteredRequests.length" class="table-wrapper">
          <table class="request-table">
            <thead>
              <tr>
                <th>신청 번호</th>
                <th>신청자</th>
                <th>리소스명</th>
                <th>카테고리</th>
                <th>수량</th>
                <th>희망일</th>
                <th>신청일</th>
                <th>상태</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredRequests" :key="item.id">
                <td>#{{ item.id }}</td>
                <td>{{ getRequesterName(item) }}</td>
                <td>{{ getResourceName(item) }}</td>
                <td>{{ getResourceCategory(item) }}</td>
                <td>{{ item.quantity ?? '-' }}</td>
                <td>{{ item.desiredDate || '-' }}</td>
                <td>{{ formatDate(item.createdAt) }}</td>
                <td><RequestStatusBadge :status="item.status" /></td>
                <td>
                  <div class="row-actions">
                    <template v-if="item.status === 'ACCEPTED'">
                      <button class="action-btn action-accept" :disabled="processingId === item.id" @click="handleStart(item)">제공 시작</button>
                      <button class="action-btn action-cancel" :disabled="processingId === item.id" @click="openReasonModal(item)">취소</button>
                    </template>
                    <template v-else-if="item.status === 'PROVISIONING'">
                      <button class="action-btn action-accept" :disabled="processingId === item.id" @click="openCompleteModal(item)">제공 완료</button>
                      <button class="action-btn action-cancel" :disabled="processingId === item.id" @click="openReasonModal(item)">취소</button>
                    </template>
                    <template v-else-if="item.status === 'RETURN_REQUESTED'">
                      <button class="action-btn action-accept" :disabled="processingId === item.id" @click="handleCollect(item)">수거 완료</button>
                    </template>
                    <template v-else-if="item.status === 'CANCEL_REQUESTED'">
                      <button class="action-btn action-accept" :disabled="processingId === item.id" @click="handleApproveCancel(item)">취소 승인</button>
                    </template>
                    <template v-else>
                      <router-link :to="`/requests/${item.id}`" class="action-btn action-view">상세보기</router-link>
                    </template>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="empty-state">
          <p>조건에 맞는 요청이 없습니다.</p>
        </div>
      </main>
    </div>

    <ReasonModal
      v-if="reasonTarget"
      title="제공 취소·회수"
      description="취소 사유를 입력해 주세요."
      :submitting="processingId === reasonTarget.id"
      @close="reasonTarget = null"
      @confirm="handleReasonConfirm"
    />

    <CompleteProvisionModal
      v-if="completeTarget"
      :submitting="processingId === completeTarget.id"
      @close="completeTarget = null"
      @confirm="handleCompleteConfirm"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import RequestStatusBadge from '@/components/RequestStatusBadge.vue'
import ReasonModal from '@/components/ReasonModal.vue'
import CompleteProvisionModal from '@/components/CompleteProvisionModal.vue'
import { requestApi } from '@/api/enrollment.js'
import { provisionApi } from '@/api/provision.js'
import { useAuthStore } from '@/store/auth.js'
import { useCourseStore } from '@/store/course.js'
import { REQUEST_STATUS } from '@/utils/requestStatus.js'
import { mapErrorMessage } from '@/utils/errorMessage.js'

const router = useRouter()
const auth = useAuthStore()
const courseStore = useCourseStore()

const requests = ref([])
const loading = ref(true)
const error = ref('')
const processingId = ref(null)

const statusFilter = ref('ALL')
const categoryFilter = ref('전체')
const searchText = ref('')

const reasonTarget = ref(null)
const completeTarget = ref(null)

const statusOptions = Object.keys(REQUEST_STATUS)

const counts = computed(() => {
  const result = { requested: 0, provided: 0, returned: 0, cancelledOrRejected: 0 }
  requests.value.forEach(item => {
    if (item.status === 'REQUESTED') {
      result.requested += 1
    } else if (item.status === 'PROVIDED' || item.status === 'RETURN_REQUESTED') {
      result.provided += 1
    } else if (item.status === 'RETURNED') {
      result.returned += 1
    } else if (['CANCELLED', 'CANCEL_REQUESTED', 'REJECTED'].includes(item.status)) {
      result.cancelledOrRejected += 1
    }
  })
  return result
})

function getRequesterName(item) {
  return item.requesterName ?? item.requester?.name ?? item.userName ?? '-'
}

function getResourceName(item) {
  return item.course?.title ?? item.resourceName ?? item.name ?? '-'
}

function getResourceCategory(item) {
  return courseStore.normalizeCategory(item.course?.category ?? item.category)
}

function getPaymentId(item) {
  return item.paymentId ?? item.provisionId ?? item.id
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

const filteredRequests = computed(() => {
  return requests.value.filter(item => {
    if (statusFilter.value !== 'ALL' && item.status !== statusFilter.value) return false
    if (categoryFilter.value !== '전체' && getResourceCategory(item) !== categoryFilter.value) return false
    if (searchText.value.trim()) {
      const keyword = searchText.value.trim().toLowerCase()
      const haystack = `${getRequesterName(item)} ${getResourceName(item)}`.toLowerCase()
      if (!haystack.includes(keyword)) return false
    }
    return true
  })
})

function handleLogout() {
  auth.logout()
  router.push('/')
}

function applyStatus(id, status) {
  const target = requests.value.find(item => item.id === id)
  if (target) target.status = status
}

async function loadRequests() {
  loading.value = true
  error.value = ''
  try {
    const res = await requestApi.getAdminList()
    console.log('[AdminRequestList] admin list response:', res.data)

    if (Array.isArray(res.data?.data)) {
      requests.value = res.data.data
    } else if (Array.isArray(res.data)) {
      requests.value = res.data
    } else {
      requests.value = []
    }
  } catch (e) {
    console.error('[AdminRequestList] failed to load requests:', e)
    error.value = mapErrorMessage(e)
    requests.value = []
  } finally {
    loading.value = false
  }
}

async function handleStart(item) {
  processingId.value = item.id
  try {
    const res = await provisionApi.start(getPaymentId(item))
    applyStatus(item.id, res.data?.data?.status ?? res.data?.status ?? 'PROVISIONING')
    await loadRequests()
  } catch (e) {
    console.error('[AdminRequestList] start failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    processingId.value = null
  }
}

async function handleCollect(item) {
  processingId.value = item.id
  try {
    const res = await provisionApi.collect(getPaymentId(item))
    applyStatus(item.id, res.data?.data?.status ?? res.data?.status ?? 'RETURNED')
    await loadRequests()
  } catch (e) {
    console.error('[AdminRequestList] collect failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    processingId.value = null
  }
}

async function handleApproveCancel(item) {
  processingId.value = item.id
  try {
    const res = await provisionApi.approveCancel(getPaymentId(item))
    applyStatus(item.id, res.data?.data?.status ?? res.data?.status ?? 'CANCELLED')
    await loadRequests()
  } catch (e) {
    console.error('[AdminRequestList] approve cancel failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    processingId.value = null
  }
}

function openReasonModal(item) {
  reasonTarget.value = item
}

async function handleReasonConfirm(reason) {
  const item = reasonTarget.value
  if (!item) return
  processingId.value = item.id
  try {
    const res = await provisionApi.cancel(getPaymentId(item), reason)
    applyStatus(item.id, res.data?.data?.status ?? res.data?.status ?? 'CANCELLED')
    reasonTarget.value = null
    await loadRequests()
  } catch (e) {
    console.error('[AdminRequestList] reason action failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    processingId.value = null
  }
}

function openCompleteModal(item) {
  completeTarget.value = item
}

async function handleCompleteConfirm(data) {
  const item = completeTarget.value
  if (!item) return
  processingId.value = item.id
  try {
    const res = await provisionApi.complete(getPaymentId(item), data)
    applyStatus(item.id, res.data?.data?.status ?? res.data?.status ?? 'PROVIDED')
    completeTarget.value = null
    await loadRequests()
  } catch (e) {
    console.error('[AdminRequestList] complete failed:', e)
    error.value = mapErrorMessage(e)
  } finally {
    processingId.value = null
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
  max-width: 1280px;
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
  margin-bottom: 20px;
}

.count-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.count-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
}

.count-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.count-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

.filter-search {
  flex: 1;
  min-width: 200px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font-sans);
  outline: none;
}

.filter-search:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.table-wrapper {
  overflow-x: auto;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.request-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  white-space: nowrap;
}

.request-table th {
  text-align: left;
  padding: 12px 14px;
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
  font-weight: 600;
  border-bottom: 1px solid var(--color-border);
}

.request-table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-primary);
}

.row-actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  text-decoration: none;
  font-family: var(--font-sans);
}

.action-accept {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.action-reject,
.action-cancel {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

.action-view {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

@media (max-width: 992px) {
  .page-layout {
    grid-template-columns: 1fr;
  }

  .count-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
