package com.lecture.payment.dto;

import com.lecture.payment.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PaymentDto {

    // API-07 요청 (Enrollment Service → Payment Service 내부 호출)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternalProvisionRequest {
        @NotNull(message = "enrollmentId는 필수입니다")
        private Long enrollmentId;

        @NotNull(message = "userId는 필수입니다")
        private Long userId;

        @NotNull(message = "courseId는 필수입니다")
        private Long courseId;

        @NotNull(message = "amount는 필수입니다")
        @Positive(message = "amount는 양수여야 합니다")
        private BigDecimal amount;
    }

    // API-07 응답
    @Getter
    @Builder
    public static class InternalProvisionResponse {
        private Long paymentId;
        private Long enrollmentId;
        private Payment.Status status;

        public static InternalProvisionResponse from(Payment payment) {
            return InternalProvisionResponse.builder()
                    .paymentId(payment.getId())
                    .enrollmentId(payment.getEnrollmentId())
                    .status(payment.getStatus())
                    .build();
        }
    }

    // API-13 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AcceptRequest {
        private String managerMemo;
    }

    // API-15 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteRequest {
        @NotBlank(message = "ticketNumber는 필수입니다")
        @Size(min = 3, max = 100, message = "ticketNumber는 3~100자여야 합니다")
        private String ticketNumber;

        @NotBlank(message = "resultMemo는 필수입니다")
        @Size(min = 5, max = 1000, message = "resultMemo는 5~1000자여야 합니다")
        private String resultMemo;
    }

    // API-16(반려), API-17(취소) 공용 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReasonRequest {
        @NotBlank(message = "reason은 필수입니다")
        @Size(min = 5, max = 500, message = "reason은 5~500자여야 합니다")
        private String reason;
    }

    // API-09, 12~17 공용 응답 ("변경된 Payment 객체")
    @Getter
    @Builder
    public static class PaymentResponse {
        private Long id;
        private Long enrollmentId;
        private Long userId;
        private Long courseId;
        private Long managerId;
        private Payment.Status status;
        private String transactionId;
        private String managerMemo;
        private String resultMemo;
        private String rejectReason;
        private String cancelReason;
        private LocalDateTime providedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static PaymentResponse from(Payment payment) {
            return PaymentResponse.builder()
                    .id(payment.getId())
                    .enrollmentId(payment.getEnrollmentId())
                    .userId(payment.getUserId())
                    .courseId(payment.getCourseId())
                    .managerId(payment.getManagerId())
                    .status(payment.getStatus())
                    .transactionId(payment.getTransactionId())
                    .managerMemo(payment.getManagerMemo())
                    .resultMemo(payment.getResultMemo())
                    .rejectReason(payment.getRejectReason())
                    .cancelReason(payment.getCancelReason())
                    .providedAt(payment.getProvidedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();
        }
    }
}
