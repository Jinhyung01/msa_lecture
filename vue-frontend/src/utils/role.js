export const isRequester = (user) => user?.role === 'STUDENT'
export const isAdmin = (user) => user?.role === 'INSTRUCTOR'

export const roleLabel = (role) => ({
  STUDENT: '신청자',
  INSTRUCTOR: '리소스 관리자',
}[role] ?? role)
