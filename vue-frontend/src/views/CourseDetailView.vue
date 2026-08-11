<template>
  <div class="page-wrapper">
    <AppHeader />

    <div class="detail-layout" v-if="course">
      <div class="detail-hero">
        <div class="detail-hero-inner">
          <!-- 좌측 상세 정보 -->
          <div class="detail-info fade-in-up">
            <div class="badge-row">
              <span class="badge" :class="style.badge">{{ course.category }}</span>
              <span class="badge" :class="course.status === 'INACTIVE' ? 'badge-neutral' : 'badge-success'">
                {{ course.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE' }}
              </span>
            </div>
            <h1 class="detail-title">{{ course.title }}</h1>
            <p class="detail-desc">
              {{ course.description || '등록된 설명이 없습니다.' }}
            </p>

            <div class="detail-meta">
              <span>제공 완료 {{ (course.enrollmentCount ?? 0).toLocaleString() }}회</span>
            </div>
          </div>

          <!-- 우측 신청 카드 -->
          <div class="enroll-card fade-in">
            <div class="enroll-thumb" :class="style.bg">
              <span class="thumb-icon">{{ style.icon }}</span>
            </div>

            <div class="enroll-body">
              <!-- 관리자는 신청 폼 숨김 -->
              <template v-if="!isAdminUser">
                <form class="request-form" @submit.prevent="handleSubmit">
                  <div class="form-group">
                    <label class="form-label">신청 사유</label>
                    <textarea
                      v-model="form.reason"
                      class="form-textarea"
                      rows="4"
                      placeholder="리소스가 필요한 이유를 10~500자로 입력해 주세요."
                      :disabled="isInactive || submitting"
                    ></textarea>
                    <div class="char-count">{{ form.reason.length }}/500</div>
                  </div>

                  <div class="form-group">
                    <label class="form-label">수량</label>
                    <input
                      v-model.number="form.quantity"
                      type="number"
                      min="1"
                      max="100"
                      class="form-input"
                      :disabled="isInactive || submitting"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">희망 제공일</label>
                    <input
                      v-model="form.desiredDate"
                      type="date"
                      :min="minDesiredDateStr"
                      class="form-input"
                      :disabled="isInactive || submitting"
                    />
                  </div>

                  <div v-if="validationError" class="error-msg">{{ validationError }}</div>
                  <div v-if="submitError" class="error-msg">{{ submitError }}</div>
                  <div v-if="submitSuccess" class="success-msg">{{ submitSuccess }}</div>

                  <button
                    type="submit"
                    class="btn btn-primary btn-full"
                    :disabled="buttonDisabled"
                    :class="{ 'btn-disabled': buttonDisabled }"
                  >
                    <span v-if="submitting">신청 중...</span>
                    <span v-else-if="isInactive">현재 신청할 수 없습니다</span>
                    <span v-else>리소스 신청</span>
                  </button>
                </form>
              </template>
              <p v-else class="helper-text">관리자 계정에는 신청 폼이 표시되지 않습니다.</p>

              <ul class="enroll-info-list">
                <li>✅ 접수 후 관리자가 검토합니다</li>
                <li>✅ 제공 완료 시 접속 정보를 안내합니다</li>
                <li>✅ 제공 이력과 취소 사유가 기록됩니다</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="loading-center">
      <div class="spinner"></div>
    </div>

    <div v-else class="loading-center">
      <p class="empty-text">리소스 정보를 불러오지 못했습니다.</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { useCourseStore } from '@/store/course.js'
import { requestApi } from '@/api/enrollment.js'
import { useAuthStore } from '@/store/auth.js'
import { isAdmin } from '@/utils/role.js'
import { mapErrorMessage } from '@/utils/errorMessage.js'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const auth = useAuthStore()

const submitting = ref(false)
const validationError = ref('')
const submitError = ref('')
const submitSuccess = ref('')

// 백엔드 EnrollRequest.desiredDate는 @Future 제약(오늘 이후, 오늘 불가)이라 최소값을 내일로 잡는다.
const tomorrow = new Date()
tomorrow.setDate(tomorrow.getDate() + 1)
const minDesiredDateStr = tomorrow.toISOString().slice(0, 10)
const form = reactive({ reason: '', quantity: 1, desiredDate: '' })

const course = computed(() => courseStore.selectedCourse)
const loading = computed(() => courseStore.loading)
const isAdminUser = computed(() => isAdmin(auth.user))
const isInactive = computed(() => course.value?.status === 'INACTIVE')

const style = computed(() => courseStore.getCategoryStyle(course.value))

const buttonDisabled = computed(() => submitting.value || isInactive.value)

function validateForm() {
  validationError.value = ''

  const reasonLength = form.reason.trim().length
  if (reasonLength < 10 || reasonLength > 500) {
    validationError.value = '신청 사유는 10~500자로 입력해 주세요.'
    return false
  }

  if (!form.quantity || form.quantity < 1 || form.quantity > 100) {
    validationError.value = '수량은 1~100 사이로 입력해 주세요.'
    return false
  }

  if (!form.desiredDate || form.desiredDate < minDesiredDateStr) {
    validationError.value = '희망 제공일은 내일 이후 날짜로 선택해 주세요.'
    return false
  }

  return true
}

async function handleSubmit() {
  submitError.value = ''
  submitSuccess.value = ''

  if (!validateForm()) return

  submitting.value = true

  try {
    await requestApi.create({
      courseId: course.value.id,
      reason: form.reason.trim(),
      quantity: Number(form.quantity),
      desiredDate: form.desiredDate
    })

    submitSuccess.value = '신청이 완료되었습니다. 내 신청 내역에서 상태를 확인해 주세요.'
    setTimeout(() => {
      router.push('/requests/my')
    }, 800)
  } catch (e) {
    console.error('[CourseDetail] request create failed:', e)
    submitError.value = mapErrorMessage(e)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await courseStore.fetchCourse(route.params.id)
})
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  background: var(--color-bg-secondary);
}

.detail-hero {
  background: linear-gradient(135deg, var(--color-hero-from) 0%, var(--color-hero-to) 100%);
  border-bottom: 1px solid var(--color-border);
  padding: 48px 0;
}

.detail-hero-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 48px;
  align-items: start;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.badge-row {
  display: flex;
  gap: 8px;
}

.detail-title {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.3;
}

.detail-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.detail-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
  flex-wrap: wrap;
}

.enroll-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.enroll-thumb {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-icon {
  font-size: 40px;
}

.thumb-teal,
.thumb-blue,
.thumb-amber,
.thumb-purple,
.thumb-pink,
.thumb-gray { background: var(--color-bg-tertiary); }

.enroll-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.request-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-input,
.form-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  resize: vertical;
}

.form-input:focus,
.form-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.char-count {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-muted);
}

.btn-full {
  width: 100%;
  padding: 13px;
  font-size: 15px;
  justify-content: center;
}

.btn-disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.enroll-info-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.enroll-info-list li {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.error-msg {
  font-size: 13px;
  color: var(--color-danger);
  padding: 8px 12px;
  background: var(--color-danger-light);
  border-radius: var(--radius-sm);
}

.success-msg {
  font-size: 13px;
  color: var(--color-success);
  padding: 8px 12px;
  background: var(--color-success-light);
  border-radius: var(--radius-sm);
}

.helper-text {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.empty-text {
  font-size: 14px;
  color: var(--color-text-muted);
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 100px 0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.badge-gray {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .detail-hero-inner {
    grid-template-columns: 1fr;
  }
}
</style>
