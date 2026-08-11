import api from './index.js'

export const requestApi = {
  create(data) {
    return api.post('/api/enrollments', data)
  },

  getMine(params) {
    return api.get('/api/enrollments/my', { params })
  },

  getById(id) {
    return api.get(`/api/enrollments/${id}`)
  },

  cancel(id, reason) {
    // api-gateway CORS 허용 메서드에 PATCH가 빠져 있어 POST로 보낸다 (백엔드는 PATCH/POST 둘 다 받음).
    return api.post(`/api/enrollments/${id}/cancel`, { reason })
  },

  return(id) {
    // api-gateway CORS 허용 메서드에 PATCH가 빠져 있어 POST로 보낸다 (백엔드는 PATCH/POST 둘 다 받음).
    return api.post(`/api/enrollments/${id}/return`)
  },

  getAdminList(params) {
    return api.get('/api/enrollments/admin', { params })
  },

  remove(id) {
    return api.delete(`/api/enrollments/${id}`)
  }
}

// 하위 호환 별칭
export const enrollmentApi = requestApi
