import api from './index.js'

export const resourceApi = {
  getAll(params) {
    return api.get('/api/courses', { params })
  },

  getById(id) {
    return api.get(`/api/courses/${id}`)
  },

  create(data) {
    return api.post('/api/courses', data)
  },

  update(id, data) {
    return api.put(`/api/courses/${id}`, data)
  },

  changeStatus(id, status) {
    // api-gateway CORS 허용 메서드에 PATCH가 빠져 있어 POST로 보낸다 (백엔드는 PATCH/POST 둘 다 받음).
    return api.post(`/api/courses/${id}/status`, { status })
  }
}

// 하위 호환 별칭
export const courseApi = resourceApi
