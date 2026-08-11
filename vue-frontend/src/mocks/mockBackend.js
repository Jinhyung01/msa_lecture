// 개발 모드 전용 인메모리 목업 백엔드
// 백엔드가 준비되기 전에 프론트엔드 화면을 모두 확인할 수 있도록
// /api/* 호출을 가로채 그럴듯한 응답을 돌려준다. (production 빌드에는 포함되지 않음)

export class MockApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.status = status
    this.code = code
  }
}

function now() {
  return new Date().toISOString()
}

function addDays(days) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

function getCurrentUser() {
  try {
    return JSON.parse(sessionStorage.getItem('user') || 'null')
  } catch {
    return null
  }
}

let nextResourceId = 10
let nextRequestId = 110

export const resources = [
  { id: 1, title: 'AWS 개발 서버', description: '신규 서비스 개발 및 테스트용 Linux VM (2vCPU/4GB)', category: 'SERVER', enrollmentCount: 12, status: 'ACTIVE' },
  { id: 2, title: 'AWS 클라우드 크레딧 100만원', description: '팀 단위로 사용할 수 있는 AWS 클라우드 크레딧입니다.', category: 'CLOUD', enrollmentCount: 5, status: 'ACTIVE' },
  { id: 3, title: 'IntelliJ IDEA Ultimate 라이선스', description: '1년 구독 라이선스, 개인 계정에 할당됩니다.', category: 'LICENSE', enrollmentCount: 34, status: 'ACTIVE' },
  { id: 4, title: '고객 데이터 조회 권한', description: '분석 목적의 고객 데이터 조회 권한입니다. 보안 등급 CONFIDENTIAL.', category: 'DATA', enrollmentCount: 8, status: 'ACTIVE' },
  { id: 5, title: 'GitHub Organization 계정', description: '사내 GitHub Organization 초대 및 계정 생성입니다.', category: 'ACCOUNT', enrollmentCount: 20, status: 'ACTIVE' },
  { id: 6, title: '사내 VPN 접속 권한', description: '재택/외부 근무 시 사내망 접속을 위한 VPN 계정입니다.', category: 'NETWORK', enrollmentCount: 15, status: 'ACTIVE' },
  { id: 7, title: '분석 DB 읽기 권한', description: '데이터 분석용 리포팅 DB의 읽기 전용 계정입니다.', category: 'SECURITY', enrollmentCount: 6, status: 'ACTIVE' },
  { id: 8, title: 'MacBook Pro 14인치', description: '개발용 노트북 지급, 반납 조건이 있습니다.', category: 'PHYSICAL_DEVICE', enrollmentCount: 3, status: 'ACTIVE' },
  { id: 9, title: '사내 세미나실 예약권', description: '분류되지 않은 기타 리소스 예시입니다.', category: 'OTHER', enrollmentCount: 2, status: 'INACTIVE' }
]

export const requests = [
  {
    id: 101, requesterId: 1, requesterName: '홍길동',
    course: { id: 1, title: 'AWS 개발 서버', category: 'SERVER' },
    reason: '결제 서비스 신규 개발 및 테스트를 위한 개발 서버가 필요합니다.',
    quantity: 1, desiredDate: addDays(7), status: 'REQUESTED', createdAt: now()
  },
  {
    id: 102, requesterId: 1, requesterName: '홍길동',
    course: { id: 3, title: 'IntelliJ IDEA Ultimate 라이선스', category: 'LICENSE' },
    reason: 'Vue/Spring 개발 도구로 사용할 IDE 라이선스가 필요합니다.',
    quantity: 1, desiredDate: addDays(3), status: 'ACCEPTED', createdAt: now()
  },
  {
    id: 103, requesterId: 1, requesterName: '홍길동',
    course: { id: 6, title: '사내 VPN 접속 권한', category: 'NETWORK' },
    reason: '재택 근무 시 사내망 접속이 필요합니다.',
    quantity: 1, desiredDate: addDays(1), status: 'PROVISIONING', createdAt: now()
  },
  {
    id: 104, requesterId: 1, requesterName: '홍길동', paymentId: 104,
    course: { id: 5, title: 'GitHub Organization 계정', category: 'ACCOUNT' },
    reason: '사내 GitHub Organization 협업을 위한 계정이 필요합니다.',
    quantity: 1, desiredDate: addDays(-5), status: 'PROVIDED', createdAt: addDays(-10),
    transactionId: 'github-org-invite-004',
    resultMemo: '초대 링크: https://github.com/org/invite/abcd1234',
    managerMemo: '온보딩 완료 후 즉시 사용 가능합니다.',
    providedAt: addDays(-4)
  },
  {
    id: 105, requesterId: 1, requesterName: '홍길동',
    course: { id: 8, title: 'MacBook Pro 14인치', category: 'PHYSICAL_DEVICE' },
    reason: '기존 노트북 노후화로 교체가 필요합니다.',
    quantity: 1, desiredDate: addDays(-2), status: 'REJECTED', createdAt: addDays(-8),
    rejectReason: '재고 부족으로 다음 분기 제공 예정입니다.'
  },
  {
    id: 106, requesterId: 1, requesterName: '홍길동',
    course: { id: 2, title: 'AWS 클라우드 크레딧 100만원', category: 'CLOUD' },
    reason: '사이드 프로젝트 인프라 비용 절감을 위해 신청합니다.',
    quantity: 1, desiredDate: addDays(-1), status: 'CANCELLED', createdAt: addDays(-6),
    cancelReason: '예산 계획 변경으로 취소합니다.'
  },
  {
    id: 107, requesterId: 3, requesterName: '김개발',
    course: { id: 1, title: 'AWS 개발 서버', category: 'SERVER' },
    reason: '추천 서비스 프로토타입 개발용 서버가 필요합니다.',
    quantity: 1, desiredDate: addDays(5), status: 'REQUESTED', createdAt: now()
  },
  {
    id: 108, requesterId: 4, requesterName: '이디자인', paymentId: 108,
    course: { id: 4, title: '고객 데이터 조회 권한', category: 'DATA' },
    reason: 'UX 리서치를 위한 고객 데이터 분석이 필요합니다.',
    quantity: 1, desiredDate: addDays(4), status: 'ACCEPTED', createdAt: addDays(-1)
  },
  {
    id: 109, requesterId: 3, requesterName: '김개발', paymentId: 109,
    course: { id: 7, title: '분석 DB 읽기 권한', category: 'SECURITY' },
    reason: '추천 알고리즘 검증을 위한 데이터 조회 권한이 필요합니다.',
    quantity: 1, desiredDate: addDays(2), status: 'PROVISIONING', createdAt: addDays(-2)
  }
]

const CANCELLABLE_REQUEST_STATUSES = ['REQUESTED']
const PROVISION_CANCELLABLE_STATUSES = ['ACCEPTED', 'PROVISIONING']

// 내부 조회는 실제 저장 객체(참조)를 반환해 그 자리에서 변경할 수 있게 하고,
// 외부(axios 응답)로 나가는 값은 항상 복사본을 반환한다.
// Vue의 ref가 "동일 참조 재할당"을 변경 없음으로 간주해 반응성이 누락되는 것을 막기 위함.
function findResource(id) {
  const resource = resources.find(r => r.id === Number(id))
  if (!resource) throw new MockApiError(404, 'RESOURCE_NOT_FOUND', '리소스 정보를 찾을 수 없습니다.')
  return resource
}

function findRequest(id) {
  const request = requests.find(r => r.id === Number(id))
  if (!request) throw new MockApiError(404, 'REQUEST_NOT_FOUND', '신청 정보를 찾을 수 없습니다.')
  return request
}

export function listResources() {
  return resources.map(r => ({ ...r }))
}

export function getResource(id) {
  return { ...findResource(id) }
}

export function createResource(data) {
  const resource = {
    id: nextResourceId++,
    title: data.title,
    description: data.description,
    category: data.category,
    enrollmentCount: 0,
    status: 'ACTIVE'
  }
  resources.push(resource)
  return { ...resource }
}

export function updateResource(id, data) {
  const resource = findResource(id)
  Object.assign(resource, data)
  return { ...resource }
}

export function changeResourceStatus(id, status) {
  const resource = findResource(id)
  resource.status = status
  return { ...resource }
}

export function createRequest(data) {
  const currentUser = getCurrentUser()
  const resource = findResource(data.courseId)

  const duplicate = requests.find(r =>
    r.requesterId === currentUser?.id &&
    r.course.id === resource.id &&
    ['REQUESTED', 'ACCEPTED', 'PROVISIONING', 'PROVIDED'].includes(r.status)
  )
  if (duplicate) {
    throw new MockApiError(409, 'DUPLICATE_REQUEST', '이미 신청 중이거나 제공받은 리소스입니다.')
  }

  const request = {
    id: nextRequestId++,
    requesterId: currentUser?.id ?? 1,
    requesterName: currentUser?.name ?? '신청자',
    course: { id: resource.id, title: resource.title, category: resource.category },
    reason: data.reason,
    quantity: Number(data.quantity ?? 1),
    desiredDate: data.desiredDate,
    status: 'REQUESTED',
    createdAt: now()
  }
  requests.push(request)
  return { ...request }
}

export function getMyRequests() {
  const currentUser = getCurrentUser()
  return requests.filter(r => r.requesterId === currentUser?.id).map(r => ({ ...r }))
}

export function getRequestById(id) {
  return { ...findRequest(id) }
}

export function cancelRequest(id, reason) {
  const request = findRequest(id)
  if (!CANCELLABLE_REQUEST_STATUSES.includes(request.status)) {
    throw new MockApiError(409, 'INVALID_STATUS_TRANSITION', '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.')
  }
  request.status = 'CANCELLED'
  request.cancelReason = reason
  request.cancelledAt = now()
  return { ...request }
}

export function getAdminRequests() {
  return requests.map(r => ({ ...r }))
}

export function acceptRequest(id, managerMemo) {
  const request = findRequest(id)
  if (request.status !== 'REQUESTED') {
    throw new MockApiError(409, 'INVALID_STATUS_TRANSITION', '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.')
  }
  request.status = 'ACCEPTED'
  request.managerMemo = managerMemo
  request.acceptedAt = now()
  return { ...request }
}

export function startProvision(id) {
  const request = findRequest(id)
  if (request.status !== 'ACCEPTED') {
    throw new MockApiError(409, 'INVALID_STATUS_TRANSITION', '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.')
  }
  request.status = 'PROVISIONING'
  request.startedAt = now()
  return { ...request }
}

export function completeProvision(id, data) {
  const request = findRequest(id)
  if (request.status !== 'PROVISIONING') {
    throw new MockApiError(409, 'INVALID_STATUS_TRANSITION', '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.')
  }
  request.status = 'PROVIDED'
  request.transactionId = data.ticketNumber
  request.resultMemo = data.resultMemo
  request.providedAt = now()

  const resource = resources.find(r => r.id === request.course.id)
  if (resource) resource.enrollmentCount += request.quantity || 1

  return { ...request }
}

export function rejectRequest(id, reason) {
  const request = findRequest(id)
  if (request.status !== 'REQUESTED') {
    throw new MockApiError(409, 'INVALID_STATUS_TRANSITION', '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.')
  }
  request.status = 'REJECTED'
  request.rejectReason = reason
  return { ...request }
}

export function cancelProvision(id, reason) {
  const request = findRequest(id)
  if (!PROVISION_CANCELLABLE_STATUSES.includes(request.status)) {
    throw new MockApiError(409, 'INVALID_STATUS_TRANSITION', '현재 상태에서는 처리할 수 없습니다. 목록을 새로고침해 주세요.')
  }
  request.previousStatus = request.status
  request.status = 'CANCELLED'
  request.cancelReason = reason
  request.cancelledAt = now()
  return { ...request }
}

export function getRelatedResources() {
  const currentUser = getCurrentUser()
  const related = resources.filter(r => [5, 6, 7].includes(r.id)).map(r => ({ ...r }))
  // GET /api/recommend/me 실제 응답 형태를 그대로 흉내낸다.
  return {
    userId: currentUser?.id ?? 1,
    basedOnCategories: ['SERVER'],
    relatedCategories: ['NETWORK', 'SECURITY', 'ACCOUNT'],
    resources: related
  }
}

export function registerUser(data) {
  return { id: Date.now(), employeeNumber: data.email, name: data.name, role: data.role }
}

export function getMe() {
  return getCurrentUser() ?? { id: 1, name: '홍길동', email: 'E20260001', role: 'STUDENT' }
}
