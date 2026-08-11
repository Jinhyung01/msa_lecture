<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-box">
      <h3 class="modal-title">제공 완료 처리</h3>
      <p class="modal-desc">실제 제공된 리소스 정보를 입력해 주세요.</p>

      <div class="form-group">
        <label class="form-label">작업 번호 <span class="required">*</span></label>
        <input v-model="form.ticketNumber" type="text" class="form-input" placeholder="예: aws-vm-dev-001" :disabled="submitting" />
      </div>

      <div class="form-group">
        <label class="form-label">제공 결과 <span class="required">*</span></label>
        <textarea
          v-model="form.resultMemo"
          class="form-textarea"
          rows="4"
          placeholder="접속 정보, 계정 정보 등 신청자에게 전달할 제공 결과를 입력해 주세요."
          :disabled="submitting"
        ></textarea>
      </div>

      <div v-if="error" class="modal-error">{{ error }}</div>

      <div class="modal-actions">
        <button class="btn btn-ghost" :disabled="submitting" @click="handleClose">닫기</button>
        <button class="btn btn-primary" :disabled="submitting" @click="handleConfirm">
          <span v-if="submitting">처리 중...</span>
          <span v-else>완료</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'

const props = defineProps({
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'confirm'])

const form = reactive({
  ticketNumber: '',
  resultMemo: ''
})

const error = ref('')

function handleClose() {
  if (props.submitting) return
  emit('close')
}

function handleConfirm() {
  const ticketLength = form.ticketNumber.trim().length
  if (ticketLength < 3 || ticketLength > 100) {
    error.value = '작업 번호는 3~100자로 입력해 주세요.'
    return
  }
  const resultLength = form.resultMemo.trim().length
  if (resultLength < 5 || resultLength > 1000) {
    error.value = '제공 결과는 5~1000자로 입력해 주세요.'
    return
  }
  error.value = ''
  emit('confirm', { ...form })
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-box {
  width: 100%;
  max-width: 440px;
  background: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-lg);
}
.modal-title {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 6px;
}
.modal-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}
.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.required {
  color: var(--color-danger);
}
.form-input,
.form-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 14px;
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
.modal-error {
  margin-top: 4px;
  font-size: 13px;
  color: var(--color-danger);
  background: var(--color-danger-light);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}
</style>
