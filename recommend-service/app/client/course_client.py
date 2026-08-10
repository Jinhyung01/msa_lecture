import httpx
import logging
from typing import List, Optional
from app.config.settings import settings
from app.model.schemas import CourseResponse, CourseCategory

logger = logging.getLogger(__name__)


class CourseServiceClient:
    """
    Course Service REST 클라이언트 (담당: 백엔드 A)
    - 연관 리소스 후보 조회
    - 개별 리소스 상세(카테고리 확인용) 조회
    """

    def __init__(self):
        self.base_url = settings.course_service_url

    async def get_course(self, course_id: int) -> Optional[CourseResponse]:
        """
        GET /api/courses/internal/{id}
        제공 완료된 리소스의 카테고리를 확인하기 위해 사용한다.
        """
        url = f"{self.base_url}/api/courses/internal/{course_id}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url)
                response.raise_for_status()
                return CourseResponse(**response.json())
        except httpx.HTTPError as e:
            logger.error(f"[CourseClient] 리소스 상세 조회 실패 - id: {course_id}, error: {e}")
            return None

    async def get_recommend_candidates(
        self,
        categories: List[CourseCategory],
        exclude_ids: List[int]
    ) -> List[CourseResponse]:
        """
        GET /api/courses/internal/recommend
        연관 카테고리 후보 조회 (제공 완료 횟수 내림차순, 이미 제공/진행 중인 리소스 제외)
        """
        if not categories:
            return []

        url = f"{self.base_url}/api/courses/internal/recommend"
        params = [("categories", c.value) for c in categories]
        for eid in exclude_ids:
            params.append(("excludeIds", eid))

        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
                return [CourseResponse(**c) for c in response.json()]
        except httpx.HTTPError as e:
            logger.error(f"[CourseClient] 연관 리소스 후보 조회 실패 - categories: {categories}, error: {e}")
            return []


course_client = CourseServiceClient()
