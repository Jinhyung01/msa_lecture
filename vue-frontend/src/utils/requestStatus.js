export const REQUEST_STATUS = {
  REQUESTED: { label: '신청 완료', color: 'blue', step: 1 },
  ACCEPTED: { label: '관리자 접수', color: 'indigo', step: 2 },
  PROVISIONING: { label: '제공 작업 중', color: 'orange', step: 3 },
  PROVIDED: { label: '제공 완료', color: 'green', step: 4 },
  RETURN_REQUESTED: { label: '반납 신청', color: 'orange', step: 5 },
  RETURNED: { label: '수거 완료', color: 'green', step: 6 },
  CANCEL_REQUESTED: { label: '취소 신청', color: 'orange', step: 0 },
  REJECTED: { label: '반려', color: 'red', step: 0 },
  CANCELLED: { label: '취소', color: 'red', step: 0 }
}
