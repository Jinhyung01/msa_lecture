<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-box">
      <h3 class="modal-title">{{ title }}</h3>
      <p class="modal-desc">{{ description }}</p>

      <textarea
        v-model="reason"
        class="modal-textarea"
        rows="5"
        minlength="5"
        maxlength="500"
        placeholder="사유를 5자 이상 입력해 주세요."
        :disabled="submitting"
      ></textarea>
      <div class="char-count">{{ reason.length }}/500</div>

      <div v-if="error" class="modal-error">{{ error }}</div>

      <div class="modal-actions">
        <button class="btn btn-ghost" :disabled="submitting" @click="handleClose">닫기</button>
        <button class="btn btn-primary" :disabled="submitting" @click="handleConfirm">
          <span v-if="submitting">처리 중...</span>
          <span v-else>확인</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  title: { type: String, default: '처리 사유 입력' },
  description: { type: String, default: '' },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'confirm'])

const reason = ref('')
const error = ref('')

function handleClose() {
  if (props.submitting) return
  emit('close')
}

function handleConfirm() {
  if (reason.value.trim().length < 5) {
    error.value = '사유는 5자 이상 입력해 주세요.'
    return
  }
  if (reason.value.trim().length > 500) {
    error.value = '사유는 500자 이하로 입력해 주세요.'
    return
  }
  error.value = ''
  emit('confirm', reason.value.trim())
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
  max-width: 420px;
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
  margin-bottom: 14px;
}
.modal-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
}
.modal-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}
.char-count {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}
.modal-error {
  margin-top: 8px;
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
  margin-top: 18px;
}
</style>
