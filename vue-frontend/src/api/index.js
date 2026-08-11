import axios from 'axios'
import { useAuthStore } from '@/store/auth.js'

const api = axios.create({
  baseURL: '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 목업 백엔드는 명시적으로 VITE_USE_MOCKS=true 로 켰을 때만 붙는다.
// 기본값은 꺼짐: 실제 API Gateway(localhost:8080)를 그대로 사용한다.
// import.meta.env.DEV가 build 시점에 상수로 치환되므로 production 빌드에는 mocks 코드가 포함되지 않는다.
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCKS === 'true') {
  import('@/mocks/mockAdapter.js').then(({ mockAdapter }) => {
    api.defaults.adapter = mockAdapter
  })
}

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      console.error('[API] 401 Unauthorized')
      console.error('[API] response data =', err.response?.data)
      console.error('[API] request url =', err.config?.url)
      // 디버깅 중에는 자동 로그아웃/리다이렉트 잠시 비활성화
      // const auth = useAuthStore()
      // auth.logout()
      // window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api