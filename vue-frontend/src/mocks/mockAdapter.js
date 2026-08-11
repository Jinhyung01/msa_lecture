// axios 커스텀 어댑터: 개발 모드에서 실제 네트워크 대신 mockBackend를 호출한다.
import * as backend from './mockBackend.js'

const MOCK_DELAY_MS = 350

const routes = [
  { method: 'get', pattern: /^\/api\/users\/me$/, handler: () => backend.getMe() },
  { method: 'post', pattern: /^\/api\/users\/register$/, handler: (c) => backend.registerUser(c.data) },

  { method: 'get', pattern: /^\/api\/courses$/, handler: () => backend.listResources() },
  { method: 'post', pattern: /^\/api\/courses$/, handler: (c) => backend.createResource(c.data) },
  { method: 'patch', pattern: /^\/api\/courses\/(\d+)\/status$/, handler: (c, [id]) => backend.changeResourceStatus(id, c.data?.status) },
  { method: 'put', pattern: /^\/api\/courses\/(\d+)$/, handler: (c, [id]) => backend.updateResource(id, c.data) },
  { method: 'get', pattern: /^\/api\/courses\/(\d+)$/, handler: (c, [id]) => backend.getResource(id) },

  { method: 'post', pattern: /^\/api\/enrollments$/, handler: (c) => backend.createRequest(c.data) },
  { method: 'get', pattern: /^\/api\/enrollments\/my$/, handler: () => backend.getMyRequests() },
  { method: 'get', pattern: /^\/api\/enrollments\/admin$/, handler: () => backend.getAdminRequests() },
  { method: 'patch', pattern: /^\/api\/enrollments\/(\d+)\/cancel$/, handler: (c, [id]) => backend.cancelRequest(id, c.data?.reason) },
  { method: 'get', pattern: /^\/api\/enrollments\/(\d+)$/, handler: (c, [id]) => backend.getRequestById(id) },

  { method: 'get', pattern: /^\/api\/payments\/admin$/, handler: () => backend.getAdminRequests() },
  { method: 'patch', pattern: /^\/api\/payments\/(\d+)\/accept$/, handler: (c, [id]) => backend.acceptRequest(id, c.data?.managerMemo) },
  { method: 'patch', pattern: /^\/api\/payments\/(\d+)\/start$/, handler: (c, [id]) => backend.startProvision(id) },
  { method: 'patch', pattern: /^\/api\/payments\/(\d+)\/complete$/, handler: (c, [id]) => backend.completeProvision(id, c.data) },
  { method: 'patch', pattern: /^\/api\/payments\/(\d+)\/reject$/, handler: (c, [id]) => backend.rejectRequest(id, c.data?.reason) },
  { method: 'patch', pattern: /^\/api\/payments\/(\d+)\/cancel$/, handler: (c, [id]) => backend.cancelProvision(id, c.data?.reason) },
  { method: 'get', pattern: /^\/api\/payments\/(\d+)$/, handler: (c, [id]) => backend.getRequestById(id) },

  { method: 'get', pattern: /^\/api\/recommend\/me$/, handler: () => backend.getRelatedResources() }
]

function buildResponse(config, data, status = 200) {
  return {
    data: { data },
    status,
    statusText: 'OK',
    headers: {},
    config,
    request: {}
  }
}

function parseBody(config) {
  if (typeof config.data === 'string') {
    try {
      return JSON.parse(config.data)
    } catch {
      return {}
    }
  }
  return config.data ?? {}
}

function buildError(config, err) {
  const error = new Error(err.message)
  error.isAxiosError = true
  error.config = config
  error.response = {
    status: err.status ?? 500,
    data: { code: err.code ?? 'INTERNAL_ERROR', message: err.message },
    config
  }
  return error
}

export function mockAdapter(config) {
  const method = (config.method || 'get').toLowerCase()
  const url = (config.url || '').split('?')[0]

  const route = routes.find(r => r.method === method && r.pattern.test(url))

  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (!route) {
        reject(buildError(config, { status: 404, code: 'RESOURCE_NOT_FOUND', message: `Mock 라우트를 찾을 수 없습니다: ${method.toUpperCase()} ${url}` }))
        return
      }

      try {
        const match = url.match(route.pattern)
        const params = match ? match.slice(1) : []
        const body = parseBody(config)
        const result = route.handler({ ...config, data: body }, params)
        resolve(buildResponse(config, result))
      } catch (err) {
        if (err instanceof backend.MockApiError) {
          reject(buildError(config, err))
        } else {
          console.error('[mockAdapter] unexpected error:', err)
          reject(buildError(config, { status: 500, code: 'INTERNAL_ERROR', message: '처리 중 오류가 발생했습니다.' }))
        }
      }
    }, MOCK_DELAY_MS)
  })
}
