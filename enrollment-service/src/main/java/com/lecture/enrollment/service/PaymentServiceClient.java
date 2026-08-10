package com.lecture.enrollment.service;

import com.lecture.enrollment.exception.EnrollmentApiException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.payment-service.url:http://payment-service:8084}")
    private String paymentServiceUrl;

    public PaymentResult requestPayment(
            Long enrollmentId,
            Long userId,
            Long courseId,
            BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(
                    enrollmentId, userId, courseId, amount);

            PaymentResult result = webClientBuilder.build()
                    .post()
                    .uri(paymentServiceUrl + "/api/payments/internal/request")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResult.class)
                    .block();

            if (result == null || result.getPaymentId() == null) {
                throw new IllegalStateException("Payment Service가 제공 작업 ID를 반환하지 않았습니다.");
            }
            if (!"REQUESTED".equals(result.getStatus())) {
                throw new IllegalStateException(
                        "제공 작업의 최초 상태가 REQUESTED가 아닙니다: " + result.getStatus());
            }

            log.info("[PaymentServiceClient] 제공 작업 생성 - enrollmentId: {}, paymentId: {}",
                    enrollmentId, result.getPaymentId());
            return result;
        } catch (Exception e) {
            log.error("[PaymentServiceClient] 제공 작업 생성 실패 - enrollmentId: {}, error: {}",
                    enrollmentId, e.getMessage(), e);
            throw EnrollmentApiException.internalError("제공 작업을 생성하지 못했습니다.");
        }
    }

    public void cancelProvision(Long paymentId, String reason) {
        try {
            webClientBuilder.build()
                    .patch()
                    .uri(paymentServiceUrl + "/api/payments/internal/{id}/cancel", paymentId)
                    .bodyValue(new InternalCancelRequest(reason, "REQUESTER"))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("[PaymentServiceClient] 신청자 취소 전달 - paymentId: {}", paymentId);
        } catch (Exception e) {
            log.error("[PaymentServiceClient] 신청자 취소 전달 실패 - paymentId: {}, error: {}",
                    paymentId, e.getMessage(), e);
            throw EnrollmentApiException.internalError("제공 작업 취소를 반영하지 못했습니다.");
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PaymentRequest {
        private Long enrollmentId;
        private Long userId;
        private Long courseId;
        private BigDecimal amount;

        PaymentRequest(Long enrollmentId, Long userId, Long courseId, BigDecimal amount) {
            this.enrollmentId = enrollmentId;
            this.userId = userId;
            this.courseId = courseId;
            this.amount = amount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PaymentResult {
        private Long paymentId;
        private Long enrollmentId;
        private String status;
    }

    @Getter
    @NoArgsConstructor
    public static class InternalCancelRequest {
        private String reason;
        private String requestedBy;

        InternalCancelRequest(String reason, String requestedBy) {
            this.reason = reason;
            this.requestedBy = requestedBy;
        }
    }
}
