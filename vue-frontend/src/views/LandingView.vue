<template>
  <div class="landing">
    <AppHeader />

    <!-- 히어로 섹션 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-content fade-in-up">
          <span class="hero-badge">사내 IT 리소스 신청·제공 플랫폼</span>
          <h1 class="hero-title">필요한 IT 리소스를<br>한곳에서 신청하세요</h1>
          <p class="hero-desc">서버, 클라우드, 라이선스, 데이터 및 IT 장비를 신청하고 제공 상태를 확인할 수 있습니다.</p>
          <div class="process-steps">
            <template v-for="(step, idx) in processSteps" :key="step">
              <span class="process-chip">{{ step }}</span>
              <span v-if="idx < processSteps.length - 1" class="process-arrow">→</span>
            </template>
          </div>
        </div>
        <div class="hero-visual fade-in">
          <img src="@/assets/images/logo/hubby_logo.svg" alt="Hubby" class="hero-mark" />
        </div>
      </div>
    </section>

    <!-- 리소스 카테고리 -->
    <section class="category-section">
      <div class="section-inner">
        <div class="section-header">
          <h2 class="section-title">신청 가능한 리소스 카테고리</h2>
          <router-link to="/resources" class="section-link">전체 보기 →</router-link>
        </div>
        <div class="category-grid">
          <router-link
            v-for="cat in categoryList"
            :key="cat"
            to="/resources"
            class="category-chip"
          >
            <span class="category-icon">{{ courseStore.getCategoryStyle({ category: cat }).icon }}</span>
            <span class="category-label">{{ cat }}</span>
          </router-link>
        </div>
      </div>
    </section>

    <!-- 특징 섹션 -->
    <section class="features-section">
      <div class="section-inner">
        <h2 class="section-title center">Hubby를 이용하면</h2>
        <div class="features-grid">
          <div v-for="f in features" :key="f.title" class="feature-card">
            <div class="feature-icon">{{ f.icon }}</div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section">
      <img src="@/assets/images/logo/hubby_banner_scene.svg" alt="Hubby" class="cta-scene" />
    </section>

    <!-- 푸터 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-logo">
          <img src="@/assets/images/logo/hubby_logo.svg" alt="Hubby" />
          <span>Hubby</span>
        </div>
        <p class="footer-copy">© 2026 Hubby. Internal IT Resource Platform.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import { useCourseStore } from '@/store/course.js'

const courseStore = useCourseStore()
const categoryList = courseStore.categories.filter(c => c !== '전체')

const processSteps = ['신청', '접수', '제공 작업', '제공 완료']

const features = [
  { icon: '📦', title: '다양한 리소스 카테고리', desc: '서버, 클라우드, 라이선스, 데이터, 계정, 보안, IT 장비까지 한곳에서 신청합니다.' },
  { icon: '🔄', title: '명확한 처리 단계', desc: '접수 → 제공 작업 → 제공 완료까지 진행 상태를 실시간으로 확인할 수 있습니다.' },
  { icon: '📋', title: '신청 내역 관리', desc: '내가 신청한 리소스의 진행 상황과 취소 사유를 한눈에 확인합니다.' },
  { icon: '🔗', title: '연관 리소스 추천', desc: '제공받은 리소스를 기반으로 함께 필요한 리소스를 추천받을 수 있습니다.' },
]
</script>

<style scoped>
.landing { background: var(--color-bg-secondary); }

/* 히어로 */
.hero {
  background: linear-gradient(135deg, var(--color-hero-from) 0%, var(--color-hero-via) 50%, var(--color-hero-to) 100%);
  border-bottom: 1px solid var(--color-border);
  padding: 80px 0 64px;
}
.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 48px;
  align-items: center;
}
.hero-badge {
  display: inline-block;
  padding: 5px 14px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 16px;
}
.hero-title {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.5px;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}
.hero-desc {
  font-size: 16px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  max-width: 460px;
  margin-bottom: 28px;
}
.process-steps {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.process-chip {
  padding: 6px 14px;
  border-radius: 20px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.process-arrow {
  color: var(--color-text-muted);
  font-size: 13px;
}

.hero-visual {
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-mark {
  width: 300px;
  height: 300px;
  object-fit: contain;
}

/* 공통 섹션 */
.section-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.section-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.section-title.center { text-align: center; margin-bottom: 40px; }
.section-link { font-size: 14px; color: var(--color-primary); font-weight: 500; }
.section-link:hover { text-decoration: underline; }

/* 카테고리 섹션 */
.category-section { padding: 64px 0; }
.category-grid {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 12px;
}
.category-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: var(--transition);
}
.category-chip:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-hover);
}
.category-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.category-label { font-size: 12px; font-weight: 500; color: var(--color-text-secondary); }

/* 특징 */
.features-section { padding: 64px 0; background: var(--color-bg-primary); }
.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.feature-card {
  padding: 28px 24px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  text-align: center;
  transition: var(--transition);
}
.feature-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.feature-icon { font-size: 32px; margin-bottom: 12px; }
.feature-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.feature-desc { font-size: 13px; color: var(--color-text-secondary); line-height: 1.6; }

/* CTA */
.cta-section {
  padding: 24px 0;
  background: linear-gradient(135deg, var(--color-hero-from) 0%, var(--color-hero-via) 100%);
  overflow: hidden;
}
.cta-scene {
  display: block;
  width: 100%;
  height: auto;
}

/* 푸터 */
.footer {
  background: var(--color-text-primary);
  padding: 32px 0;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}
.footer-logo img { width: 28px; height: 28px; border-radius: 6px; }
.footer-copy { font-size: 13px; color: rgba(255,255,255,0.5); }

@media (max-width: 992px) {
  .category-grid { grid-template-columns: repeat(5, 1fr); }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .hero-inner { grid-template-columns: 1fr; }
  .hero-visual { display: none; }
  .category-grid { grid-template-columns: repeat(3, 1fr); }
  .features-grid { grid-template-columns: 1fr; }
}
</style>
