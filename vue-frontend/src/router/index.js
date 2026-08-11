import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('@/views/LandingView.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('@/views/CallbackView.vue')
  },
  {
    path: '/resources',
    name: 'ResourceList',
    component: () => import('@/views/CourseListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/resources/new',
    name: 'ResourceCreate',
    component: () => import('@/views/CourseCreateView.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/resources/:id(\\d+)',
    name: 'ResourceDetail',
    component: () => import('@/views/CourseDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/related',
    name: 'RelatedResources',
    component: () => import('@/views/RelatedResourcesView.vue'),
    meta: { requiresAuth: true, requesterOnly: true }
  },
  {
    path: '/requests/my',
    name: 'MyRequests',
    component: () => import('@/views/EnrollmentView.vue'),
    meta: { requiresAuth: true, requesterOnly: true }
  },
  {
    path: '/requests/:id(\\d+)',
    name: 'RequestDetail',
    component: () => import('@/views/RequestDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/requests',
    name: 'AdminRequests',
    component: () => import('@/views/AdminRequestListView.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/mypage',
    name: 'MyPage',
    component: () => import('@/views/MyPageView.vue'),
    meta: { requiresAuth: true }
  },

  // 기존 URL 호환용 redirect
  { path: '/courses', redirect: '/resources' },
  { path: '/courses/new', redirect: '/resources/new' },
  { path: '/courses/:id(\\d+)', redirect: (to) => `/resources/${to.params.id}` },
  { path: '/enrollments', redirect: '/requests/my' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 인증/권한 가드
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'Login' }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return auth.user?.role === 'INSTRUCTOR'
      ? { name: 'AdminRequests' }
      : { name: 'ResourceList' }
  }

  if (to.meta.adminOnly && auth.user?.role !== 'INSTRUCTOR') {
    return { name: 'ResourceList' }
  }

  if (to.meta.requesterOnly && auth.user?.role !== 'STUDENT') {
    return { name: 'AdminRequests' }
  }
})

export default router
