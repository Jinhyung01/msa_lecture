const ERROR_MESSAGE_MAP = {
  VALIDATION_ERROR: '입력값을 다시 확인해 주세요.',
  UNAUTHORIZED: '로그인이 필요합니다.',
  DUPLICATE_REQUEST: '이미 신청 중이거나 제공받은 리소스입니다.',
  INVALID_STATUS_TRANSITION: '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.',
  FORBIDDEN: '이 작업을 수행할 권한이 없습니다.',
  RESOURCE_NOT_FOUND: '리소스 정보를 찾을 수 없습니다.',
  REQUEST_NOT_FOUND: '신청 정보를 찾을 수 없습니다.',
  PROVISION_NOT_FOUND: '제공 작업 정보를 찾을 수 없습니다.',
  INTERNAL_ERROR: '처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
}

export function mapErrorMessage(error) {
  const code = error?.response?.data?.code
  if (code && ERROR_MESSAGE_MAP[code]) {
    return ERROR_MESSAGE_MAP[code]
  }
  return error?.response?.data?.message || '처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
}
