package com.lecture.enrollment.dto;

import com.lecture.enrollment.entity.Enrollment;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EnrollmentDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollRequest {
        @NotNull(message = "리소스 ID는 필수입니다.")
        private Long courseId;

        @NotBlank(message = "신청 사유는 필수입니다.")
        @Size(min = 10, max = 500, message = "신청 사유는 10자 이상 500자 이하로 입력해 주세요.")
        private String reason;

        @NotNull(message = "신청 수량은 필수입니다.")
        @Min(value = 1, message = "신청 수량은 1 이상이어야 합니다.")
        @Max(value = 100, message = "신청 수량은 100 이하여야 합니다.")
        private Integer quantity;

        @Future(message = "희망 제공일은 오늘 이후여야 합니다.")
        private LocalDate desiredDate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelRequest {
        @NotBlank(message = "취소 사유는 필수입니다.")
        @Size(min = 5, max = 500, message = "취소 사유는 5자 이상 500자 이하로 입력해 주세요.")
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseSummary {
        private Long id;
        private String title;
        private String description;
        private String category;
        private BigDecimal price;
        private String thumbnail;
        private String instructorName;
        private Integer enrollmentCount;
        private String status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentResponse {
        private Long id;
        private Long userId;
        private String userName;
        private String userIdentifier;
        private Long courseId;
        private Long paymentId;
        private String reason;
        private Integer quantity;
        private LocalDate desiredDate;
        private Enrollment.Status status;
        private String rejectReason;
        private String cancelReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private CourseSummary course;

        public static EnrollmentResponse from(Enrollment enrollment) {
            return from(enrollment, null);
        }

        public static EnrollmentResponse from(Enrollment enrollment, CourseSummary course) {
            return from(enrollment, course, null, null);
        }

        public static EnrollmentResponse from(
                Enrollment enrollment,
                CourseSummary course,
                String userName,
                String userIdentifier) {
            return EnrollmentResponse.builder()
                    .id(enrollment.getId())
                    .userId(enrollment.getUserId())
                    .userName(userName)
                    .userIdentifier(userIdentifier)
                    .courseId(enrollment.getCourseId())
                    .paymentId(enrollment.getPaymentId())
                    .reason(enrollment.getReason())
                    .quantity(enrollment.getQuantity())
                    .desiredDate(enrollment.getDesiredDate())
                    .status(enrollment.getStatus())
                    .rejectReason(enrollment.getRejectReason())
                    .cancelReason(enrollment.getCancelReason())
                    .createdAt(enrollment.getCreatedAt())
                    .updatedAt(enrollment.getUpdatedAt())
                    .course(course)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentHistoryResponse {
        private Long userId;
        private List<Long> providedCourseIds;

        /**
         * 기존 Recommend Service와의 전환기 호환 필드.
         * providedCourseIds와 같은 값을 반환한다.
         */
        private List<Long> activeCourseIds;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }
    }
}
