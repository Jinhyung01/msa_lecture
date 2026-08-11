import { defineStore } from 'pinia'
import { ref } from 'vue'
import { resourceApi } from '@/api/course.js'
import { mapErrorMessage } from '@/utils/errorMessage.js'

export const useCourseStore = defineStore('course', () => {
  const courses = ref([])
  const selectedCourse = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const selectedCategory = ref('전체')

  const categories = ['전체', '서버', '클라우드', '라이선스', '데이터', '계정', '네트워크', '보안', 'IT 장비', '기타']

  // 백엔드 카테고리 코드 → 프론트 표시용 한글 카테고리
  const categoryLabelMap = {
    SERVER: '서버',
    CLOUD: '클라우드',
    LICENSE: '라이선스',
    DATA: '데이터',
    ACCOUNT: '계정',
    NETWORK: '네트워크',
    SECURITY: '보안',
    PHYSICAL_DEVICE: 'IT 장비',
    OTHER: '기타'
  }

  // 카테고리별 아이콘/색상 (썸네일 이미지 대체)
  const categoryIconMap = {
    '서버': { icon: '🖥️', bg: 'thumb-teal', badge: 'badge-teal' },
    '클라우드': { icon: '☁️', bg: 'thumb-blue', badge: 'badge-blue' },
    '라이선스': { icon: '📜', bg: 'thumb-purple', badge: 'badge-purple' },
    '데이터': { icon: '🗄️', bg: 'thumb-purple', badge: 'badge-purple' },
    '계정': { icon: '👤', bg: 'thumb-amber', badge: 'badge-amber' },
    '네트워크': { icon: '🌐', bg: 'thumb-blue', badge: 'badge-blue' },
    '보안': { icon: '🔒', bg: 'thumb-pink', badge: 'badge-pink' },
    'IT 장비': { icon: '💻', bg: 'thumb-teal', badge: 'badge-teal' },
    '기타': { icon: '📦', bg: 'thumb-gray', badge: 'badge-gray' }
  }

  function normalizeCategory(category) {
    if (!category) return ''
    return categoryLabelMap[category] || category
  }

  function normalizeCourse(course) {
    if (!course || typeof course !== 'object') return course

    return {
      ...course,
      category: normalizeCategory(course.category)
    }
  }

  function getCategoryIcon(course) {
    return categoryIconMap[course?.category]?.icon || '📦'
  }

  function getCategoryStyle(course) {
    return categoryIconMap[course?.category] || categoryIconMap['기타']
  }

  async function fetchCourses() {
    loading.value = true
    error.value = null

    try {
      const res = await resourceApi.getAll()
      console.log('[CourseStore] fetchCourses response =', res.data)

      const rawCourses = Array.isArray(res.data?.data)
        ? res.data.data
        : Array.isArray(res.data)
          ? res.data
          : []

      courses.value = rawCourses.map(normalizeCourse)

      console.log('[CourseStore] normalized courses =', courses.value)
    } catch (e) {
      console.error('[CourseStore] fetchCourses failed:', e)
      error.value = mapErrorMessage(e)
      courses.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchCourse(id) {
    loading.value = true
    error.value = null

    try {
      const res = await resourceApi.getById(id)
      console.log('[CourseStore] fetchCourse response =', res.data)

      const rawCourse =
        res.data?.data && typeof res.data.data === 'object'
          ? res.data.data
          : res.data

      selectedCourse.value = normalizeCourse(rawCourse)

      console.log('[CourseStore] normalized selectedCourse =', selectedCourse.value)
    } catch (e) {
      console.error('[CourseStore] fetchCourse failed:', e)
      error.value = mapErrorMessage(e)
      selectedCourse.value = null
    } finally {
      loading.value = false
    }
  }

  function setCategory(cat) {
    selectedCategory.value = cat
  }

  return {
    courses,
    selectedCourse,
    loading,
    error,
    categories,
    selectedCategory,
    categoryLabelMap,
    normalizeCategory,
    normalizeCourse,
    getCategoryIcon,
    getCategoryStyle,
    fetchCourses,
    fetchCourse,
    setCategory
  }
})
