import logging
from fastapi import APIRouter, Header
from app.model.schemas import RelatedResourceResponse
from app.service.recommend_service import recommend_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/recommend", tags=["recommend"])


@router.get("/me", response_model=RelatedResourceResponse)
async def get_related_resources(
    x_user_id: int = Header(..., alias="X-User-Id")
):
    """
    GET /api/recommend/me - 연관 리소스 안내 (API-18)
    다른 서비스와 동일하게 Gateway가 전달한 X-User-Id 헤더를 사용한다
    (body/JWT의 userId는 신뢰하지 않는다 - 명세서 6.1).
    """
    logger.info(f"[Router] 연관 리소스 요청 - userId: {x_user_id}")
    return await recommend_service.get_related_resources(x_user_id)


@router.get("/health", include_in_schema=False)
async def health_check():
    return {"status": "UP", "service": "recommend-service"}
