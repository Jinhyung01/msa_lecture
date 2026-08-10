package com.lecture.course.dto;

import com.lecture.course.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CourseDto {

    /** 리소스 등록/수정 요청 (API-04, API-05 공통 - 명세서: "요청은 API-04와 동일하다") */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveRequest {

        @NotBlank(message = "리소스명은 필수입니다")
        @Size(min = 2, max = 100, message = "리소스명은 2~100자여야 합니다")
        private String title;

        @NotBlank(message = "설명은 필수입니다")
        @Size(min = 10, max = 2000, message = "설명은 10~2000자여야 합니다")
        private String description;

        @NotNull(message = "카테고리는 필수입니다")
        private Course.Category category;

        @NotNull(message = "예상 비용은 필수입니다")
        @PositiveOrZero(message = "예상 비용은 0 이상이어야 합니다")
        private BigDecimal price;
    }

    /** 신청 가능 상태 변경 요청 (API: PATCH /api/courses/{id}/status) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateRequest {
        @NotNull(message = "status는 필수입니다")
        private Course.Status status;
    }

    /** 리소스 응답 - 래퍼 없이 그대로 반환한다 (명세서 6.3) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseResponse {
        private Long id;
        private String title;
        private String description;
        private Course.Category category;
        private BigDecimal price;
        private Long instructorId;
        private Integer enrollmentCount;
        private Course.Status status;
        private LocalDateTime createdAt;

        public static CourseResponse from(Course course) {
            return CourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .category(course.getCategory())
                    .price(course.getPrice())
                    .instructorId(course.getInstructorId())
                    .enrollmentCount(course.getEnrollmentCount())
                    .status(course.getStatus())
                    .createdAt(course.getCreatedAt())
                    .build();
        }
    }

    /** 연관 리소스 안내(API-18)에서 Recommend Service가 소비하는 후보 목록 응답 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendCandidateResponse {
        private List<CourseResponse> resources;
    }
}
