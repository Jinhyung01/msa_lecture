from pydantic import BaseModel
from typing import List, Optional
from enum import Enum
from decimal import Decimal
from datetime import datetime


class CourseCategory(str, Enum):
    """통합 개발 구현 명세서 5.2 확정 리소스 카테고리 Enum"""
    SERVER = "SERVER"
    CLOUD = "CLOUD"
    LICENSE = "LICENSE"
    DATA = "DATA"
    ACCOUNT = "ACCOUNT"
    NETWORK = "NETWORK"
    SECURITY = "SECURITY"
    PHYSICAL_DEVICE = "PHYSICAL_DEVICE"
    OTHER = "OTHER"


class CourseResponse(BaseModel):
    id: int
    title: str
    description: Optional[str] = None
    category: CourseCategory
    price: Decimal
    instructorId: int
    enrollmentCount: int
    status: str
    createdAt: Optional[datetime] = None


class EnrollmentHistoryResponse(BaseModel):
    """
    GET /api/enrollments/internal/history/{userId} (Enrollment Service, 백엔드 B 제공) 응답 계약.
    providedCourseIds: PROVIDED 상태로 완료된 리소스 ID 목록 (연관 리소스 산정 기준)
    inProgressCourseIds: REQUESTED/ACCEPTED/PROVISIONING 등 진행 중인 리소스 ID 목록 (추천 후보에서 제외)
    """
    userId: int
    providedCourseIds: List[int] = []
    inProgressCourseIds: List[int] = []


class RelatedResourceResponse(BaseModel):
    """API-18 GET /api/recommend/me 응답"""
    userId: int
    basedOnCategories: List[CourseCategory] = []
    relatedCategories: List[CourseCategory] = []
    resources: List[CourseResponse] = []
