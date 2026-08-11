import api from './index.js'

// api-gateway CORS 허용 메서드 목록(GET,POST,PUT,DELETE,OPTIONS)에 PATCH가 빠져 있어
// 브라우저에서 PATCH 호출이 전부 403으로 막힌다 (소스 없는 사전 빌드 이미지라 직접 못 고침).
// 게이트웨이가 고쳐지기 전까지 아래는 전부 POST로 보낸다 (백엔드는 PATCH/POST 둘 다 받음).
export const provisionApi = {
  getAll(params) {
    return api.get('/api/payments/admin', { params })
  },

  getById(id) {
    return api.get(`/api/payments/${id}`)
  },

  accept(id, managerMemo) {
    return api.post(`/api/payments/${id}/accept`, { managerMemo })
  },

  start(id) {
    return api.post(`/api/payments/${id}/start`)
  },

  complete(id, data) {
    return api.post(`/api/payments/${id}/complete`, data)
  },

  reject(id, reason) {
    return api.post(`/api/payments/${id}/reject`, { reason })
  },

  cancel(id, reason) {
    return api.post(`/api/payments/${id}/cancel`, { reason })
  },

  collect(id) {
    return api.post(`/api/payments/${id}/collect`)
  },

  approveCancel(id) {
    return api.post(`/api/payments/${id}/approve-cancel`)
  }
}
