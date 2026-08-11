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

          <router-link
            v-if="isAdminUser"
            to="/admin/requests"
            class="sidebar-item"
          >
            <span class="si-icon">🗂️</span> 요청 관리
          </router-link>

          <router-link
            v-else
            to="/requests/my"
            class="sidebar-item"
          >
            <span class="si-icon">✅</span> 내 신청 내역
          </router-link>

          <router-link
            v-if="!isAdminUser"
            to="/related"
            class="sidebar-item"
          >
            <span class="si-icon">🔗</span> 연관 리소스
          </router-link>

          <router-link to="/mypage" class="sidebar-item active">
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
        <!-- 프로필 카드 -->
        <div class="profile-card fade-in-up">
          <div class="profile-avatar">{{ auth.user?.name?.charAt(0) || '?' }}</div>
          <div class="profile-info">
            <h2 class="profile-name">{{ auth.user?.name || '사용자' }}</h2>
            <p class="profile-employee-number">이메일 주소: {{ auth.user?.email || '-' }}</p>
            <span class="badge" :class="isAdminUser ? 'badge-amber' : 'badge-blue'">
              {{ roleLabel(auth.user?.role) }}
            </span>
          </div>
        </div>

        <!-- 신청자 통계 -->
        <section v-if="!isAdminUser" class="stats-section">
          <h3 class="section-title">내 신청 현황</h3>

          <div v-if="statsLoading" class="loading-row">
            <div v-for="i in 3" :key="i" class="skeleton-card"></div>
          </div>

          <div v-else class="summary-cards">
            <div class="summary-card">
              <div class="summary-label">신청 대기</div>
              <div class="summary-value">{{ pendingCount }}</div>
              <ul class="summary-detail-list">
                <li v-for="item in pendingList" :key="item.id">
                  <router-link :to="`/requests/${item.id}`">- {{ getResourceName(item) }} × {{ item.quantity ?? 1 }}</router-link>
                </li>
              </ul>
            </div>
            <div class="summary-card">
              <div class="summary-label">진행 중 건수</div>
              <div class="summary-value">{{ inProgressCount }}</div>
              <ul class="summary-detail-list">
                <li v-for="item in inProgressList" :key="item.id">
                  <router-link :to="`/requests/${item.id}`">- {{ getResourceName(item) }} × {{ item.quantity ?? 1 }}</router-link>
                </li>
              </ul>
            </div>
            <div class="summary-card">
              <div class="summary-label">제공 완료 건수</div>
              <div class="summary-value">{{ providedCount }}</div>
              <ul class="summary-detail-list">
                <li v-for="item in providedList" :key="item.id">
                  <router-link :to="`/requests/${item.id}`">- {{ getResourceName(item) }} × {{ item.quantity ?? 1 }}</router-link>
                </li>
              </ul>
            </div>
          </div>

          <p v-if="statsError" class="empty-text">{{ statsError }}</p>
        </section>

        <!-- 관리자 안내 -->
        <section v-else class="admin-section">
          <h3 class="section-title">관리자 메뉴</h3>
          <p class="admin-desc">요청 관리 화면에서 대기 중인 리소스 요청을 접수하고 제공 작업을 처리할 수 있습니다.</p>
          <router-link to="/admin/requests" class="btn btn-primary">요청 관리로 이동</router-link>

          <div class="history-block">
            <h4 class="history-title">내 처리 내역</h4>
            <div v-if="historyLoading" class="history-loading-rows">
              <div v-for="i in 3" :key="i" class="skeleton-card history-skeleton"></div>
            </div>
            <ul v-else-if="history.length" class="history-list">
              <li v-for="h in history" :key="h.id">
                <router-link :to="`/requests/${h.id}`" class="history-link">
                  <span class="history-action">{{ h.actionLabel }}</span>
                  <span class="history-name">{{ h.resourceName }}</span>
                  <span class="history-date">{{ h.date }}</span>
                </router-link>
              </li>
            </ul>
            <p v-else class="empty-text">아직 처리한 내역이 없습니다.</p>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { useAuthStore } from '@/store/auth.js'
import { requestApi } from '@/api/enrollment.js'
import { provisionApi } from '@/api/provision.js'
import { isAdmin, roleLabel } from '@/utils/role.js'
import { mapErrorMessage } from '@/utils/errorMessage.js'

const ADMIN_ACTION_LABELS = {
  ACCEPTED: '접수함',
  PROVISIONING: '제공 시작함',
  PROVIDED: '제공 완료 처리함',
  REJECTED: '반려함',
  CANCELLED: '취소 처리함',
  RETURNED: '수거 완료함'
}

const router = useRouter()
const auth = useAuthStore()

const isAdminUser = computed(() => isAdmin(auth.user))

const allList = ref([])
const statsLoading = ref(true)
const statsError = ref('')

// 같은 리소스를 여러 번 신청한 경우 리소스당 가장 최근 건만 남기고,
// 그 최근 건이 취소된 상태라면 목록에서 완전히 제외한다.
const dedupedList = computed(() => {
  const latestByResource = new Map()
  for (const item of allList.value) {
    const key = item.course?.id ?? item.courseId
    const existing = latestByResource.get(key)
    if (!existing || item.id > existing.id) {
      latestByResource.set(key, item)
    }
  }
  return Array.from(latestByResource.values()).filter(item => item.status !== 'CANCELLED')
})

const pendingList = computed(() =>
  dedupedList.value.filter(item => item.status === 'REQUESTED')
)
const pendingCount = computed(() => pendingList.value.length)
const inProgressList = computed(() =>
  dedupedList.value.filter(item => ['ACCEPTED', 'PROVISIONING'].includes(item.status))
)
const inProgressCount = computed(() => inProgressList.value.length)
const providedList = computed(() =>
  dedupedList.value.filter(item => ['PROVIDED', 'RETURN_REQUESTED'].includes(item.status))
)
const providedCount = computed(() => providedList.value.length)

const history = ref([])
const historyLoading = ref(true)

function getResourceName(item) {
  return item.course?.title ?? item.resourceName ?? item.name ?? '-'
}

function handleLogout() {
  auth.logout()
  router.push('/')
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await requestApi.getAdminList()
    const list = Array.isArray(res.data?.data)
      ? res.data.data
      : Array.isArray(res.data)
        ? res.data
        : []

    const actedItems = list.filter(item => ADMIN_ACTION_LABELS[item.status] && item.paymentId)
    const withManager = await Promise.all(
      actedItems.map(async item => {
        try {
          const pRes = await provisionApi.getById(item.paymentId)
          const payment = pRes.data?.data ?? pRes.data ?? null
          return { item, managerId: payment?.managerId ?? null }
        } catch {
          return { item, managerId: null }
        }
      })
    )

    history.value = withManager
      .filter(({ managerId }) => managerId === auth.user?.id)
      .map(({ item }) => ({
        id: item.id,
        resourceName: getResourceName(item),
        actionLabel: ADMIN_ACTION_LABELS[item.status],
        date: (item.updatedAt || item.createdAt || '').slice(0, 10)
      }))
      .sort((a, b) => (a.date < b.date ? 1 : -1))
      .slice(0, 10)
  } catch (e) {
    console.error('[MyPage] failed to load admin history:', e)
  } finally {
    historyLoading.value = false
  }
}

async function loadStats() {
  statsLoading.value = true
  statsError.value = ''
  try {
    const res = await requestApi.getMine()
    allList.value = Array.isArray(res.data?.data)
      ? res.data.data
      : Array.isArray(res.data)
        ? res.data
        : []
  } catch (e) {
    console.error('[MyPage] failed to load request stats:', e)
    statsError.value = mapErrorMessage(e)
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  if (isAdminUser.value) {
    loadHistory()
  } else {
    loadStats()
  }
})
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
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-sm);
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
}

.profile-employee-number {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.badge-blue {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
}

.badge-amber {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 16px;
}

.summary-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
}

.summary-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 8px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.summary-detail-list {
  list-style: none;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 140px;
  overflow-y: auto;
}

.summary-detail-list a {
  display: block;
  font-size: 12px;
  color: var(--color-text-secondary);
  text-decoration: none;
}

.summary-detail-list a:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.loading-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.skeleton-card {
  height: 90px;
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.admin-section {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.admin-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

.empty-text {
  color: var(--color-text-muted);
  font-size: 14px;
  margin-top: 12px;
}

.history-block {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}

.history-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 12px;
}

.history-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: var(--transition);
}

.history-link:hover {
  background: var(--color-bg-tertiary);
}

.history-action {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  flex-shrink: 0;
}

.history-name {
  flex: 1;
  font-size: 13px;
  color: var(--color-text-primary);
}

.history-date {
  font-size: 12px;
  color: var(--color-text-muted);
}

.history-loading-rows {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-skeleton {
  height: 40px;
  border-radius: var(--radius-md);
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 992px) {
  .page-layout {
    grid-template-columns: 1fr;
  }

  .summary-cards,
  .loading-row {
    grid-template-columns: 1fr;
  }
}
</style>
