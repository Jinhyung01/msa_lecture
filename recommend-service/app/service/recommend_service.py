import logging
from typing import Dict, List

from app.client.course_client import course_client
from app.client.enrollment_client import enrollment_client
from app.model.schemas import CourseCategory, RelatedResourceResponse

logger = logging.getLogger(__name__)

# 카테고리 연관 규칙 (규칙 기반, AI 미사용 - 플로우 문서 16장 규칙을
# 통합 개발 구현 명세서 5.2의 확정 카테고리 Enum(SERVER/CLOUD/LICENSE/DATA/
# ACCOUNT/NETWORK/SECURITY/PHYSICAL_DEVICE/OTHER)에 맞춰 옮긴 것이다.
# 플로우 문서의 DATABASE 카테고리가 최종 Enum에는 없어 SECURITY로 대체했다.
# 실제 값은 팀 합의 후 조정 가능하다.
CATEGORY_RELATION_MAP: Dict[CourseCategory, List[CourseCategory]] = {
    CourseCategory.SERVER: [CourseCategory.NETWORK, CourseCategory.SECURITY, CourseCategory.ACCOUNT],
    CourseCategory.CLOUD: [CourseCategory.ACCOUNT, CourseCategory.NETWORK, CourseCategory.SECURITY],
    CourseCategory.DATA: [CourseCategory.SECURITY, CourseCategory.ACCOUNT],
    CourseCategory.PHYSICAL_DEVICE: [CourseCategory.LICENSE, CourseCategory.ACCOUNT],
    CourseCategory.LICENSE: [CourseCategory.ACCOUNT],
    CourseCategory.NETWORK: [CourseCategory.SECURITY, CourseCategory.ACCOUNT],
    CourseCategory.ACCOUNT: [CourseCategory.SECURITY],
    CourseCategory.SECURITY: [CourseCategory.ACCOUNT],
    CourseCategory.OTHER: [],
}

MAX_RESOURCE_COUNT = 5  # 최대 연관 리소스 수


class RecommendService:
    """
    규칙 기반 연관 리소스 안내 서비스 (API-18)

    처리 순서:
    1. Enrollment Service에서 사용자의 PROVIDED / 진행 중 리소스 ID 조회
    2. PROVIDED 리소스들의 카테고리 추출 (Course Service 조회)
    3. 카테고리 연관 규칙 적용
    4. Course Service에서 ACTIVE 후보 조회
    5. 이미 PROVIDED 또는 진행 중인 리소스 제외
    6. enrollmentCount(제공 완료 횟수) 내림차순, 최대 5개 반환
    """

    async def get_related_resources(self, user_id: int) -> RelatedResourceResponse:
        logger.info(f"[RecommendService] 연관 리소스 조회 시작 - userId: {user_id}")

        history = await enrollment_client.get_enrollment_history(user_id)

        # PROVIDED 이력이 없으면 빈 배열을 반환한다 (명세서 8. API-18)
        if not history.providedCourseIds:
            return RelatedResourceResponse(userId=user_id)

        based_on_categories = await self._resolve_categories(history.providedCourseIds)
        if not based_on_categories:
            return RelatedResourceResponse(userId=user_id)

        related_categories = self._apply_relation_rule(based_on_categories)
        if not related_categories:
            return RelatedResourceResponse(
                userId=user_id,
                basedOnCategories=based_on_categories,
            )

        exclude_ids = list(set(history.providedCourseIds) | set(history.inProgressCourseIds))

        candidates = await course_client.get_recommend_candidates(
            categories=related_categories,
            exclude_ids=exclude_ids,
        )
        resources = candidates[:MAX_RESOURCE_COUNT]

        logger.info(
            f"[RecommendService] 연관 리소스 조회 완료 - userId: {user_id}, "
            f"basedOn: {based_on_categories}, related: {related_categories}, count: {len(resources)}"
        )

        return RelatedResourceResponse(
            userId=user_id,
            basedOnCategories=based_on_categories,
            relatedCategories=related_categories,
            resources=resources,
        )

    async def _resolve_categories(self, course_ids: List[int]) -> List[CourseCategory]:
        """제공 완료된 리소스 ID 목록 → 중복 제거된 카테고리 목록"""
        categories: List[CourseCategory] = []
        for course_id in course_ids:
            course = await course_client.get_course(course_id)
            if course and course.category not in categories:
                categories.append(course.category)
        return categories

    def _apply_relation_rule(self, based_on_categories: List[CourseCategory]) -> List[CourseCategory]:
        related: List[CourseCategory] = []
        for category in based_on_categories:
            for related_category in CATEGORY_RELATION_MAP.get(category, []):
                if related_category not in related and related_category not in based_on_categories:
                    related.append(related_category)
        return related


recommend_service = RecommendService()
