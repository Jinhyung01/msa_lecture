import api from './index.js'

export const relatedResourceApi = {
  getMine() {
    return api.get('/api/recommend/me')
  }
}
