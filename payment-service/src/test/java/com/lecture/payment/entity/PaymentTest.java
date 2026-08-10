package com.lecture.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lecture.payment.exception.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private Payment newRequestedPayment() {
        return Payment.builder()
                .enrollmentId(101L)
                .userId(1L)
                .courseId(10L)
                .amount(BigDecimal.valueOf(100_000))
                .build();
    }

    @Test
    void internal_provision_starts_requested_without_a_ticket_number() {
        Payment payment = newRequestedPayment();

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.REQUESTED);
        assertThat(payment.getTransactionId()).isNull();
    }

    @Test
    void accept_moves_requested_to_accepted_and_stores_manager() {
        Payment payment = newRequestedPayment();

        payment.accept(2L, "할당 가능한 VM 자원을 확인했습니다.");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.ACCEPTED);
        assertThat(payment.getManagerId()).isEqualTo(2L);
        assertThat(payment.getManagerMemo()).isEqualTo("할당 가능한 VM 자원을 확인했습니다.");
    }

    @Test
    void accept_rejects_a_payment_that_is_not_requested() {
        Payment payment = newRequestedPayment();
        payment.accept(2L, "memo");

        assertThatThrownBy(() -> payment.accept(2L, "again"))
                .isInstanceOf(ApiException.class);
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.ACCEPTED);
    }

    @Test
    void start_moves_accepted_to_provisioning() {
        Payment payment = newRequestedPayment();
        payment.accept(2L, "memo");

        payment.start(2L);

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PROVISIONING);
    }

    @Test
    void start_rejects_a_payment_that_is_not_accepted() {
        Payment payment = newRequestedPayment();

        assertThatThrownBy(() -> payment.start(2L))
                .isInstanceOf(ApiException.class);
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.REQUESTED);
    }

    @Test
    void complete_moves_provisioning_to_provided_and_stores_result() {
        Payment payment = newRequestedPayment();
        payment.accept(2L, "memo");
        payment.start(2L);

        payment.complete(2L, "SRV-2026-0001", "개발 서버와 SSH 계정 발급 완료.");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PROVIDED);
        assertThat(payment.getTransactionId()).isEqualTo("SRV-2026-0001");
        assertThat(payment.getResultMemo()).isEqualTo("개발 서버와 SSH 계정 발급 완료.");
        assertThat(payment.getProvidedAt()).isNotNull();
    }

    @Test
    void complete_rejects_a_payment_that_is_still_requested() {
        Payment payment = newRequestedPayment();

        assertThatThrownBy(() -> payment.complete(2L, "SRV-1", "memo"))
                .isInstanceOf(ApiException.class);
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.REQUESTED);
    }

    @Test
    void reject_moves_requested_to_rejected_and_stores_reason() {
        Payment payment = newRequestedPayment();

        payment.reject(2L, "현재 할당 가능한 서버 자원이 없습니다.");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.REJECTED);
        assertThat(payment.getRejectReason()).isEqualTo("현재 할당 가능한 서버 자원이 없습니다.");
    }

    @Test
    void reject_rejects_a_payment_that_is_not_requested() {
        Payment payment = newRequestedPayment();
        payment.accept(2L, "memo");

        assertThatThrownBy(() -> payment.reject(2L, "reason"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cancel_moves_accepted_to_cancelled() {
        Payment payment = newRequestedPayment();
        payment.accept(2L, "memo");

        payment.cancel(2L, "보안 검토 결과 제공 작업이 취소되었습니다.");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.CANCELLED);
        assertThat(payment.getCancelReason()).isEqualTo("보안 검토 결과 제공 작업이 취소되었습니다.");
    }

    @Test
    void cancel_moves_provisioning_to_cancelled() {
        Payment payment = newRequestedPayment();
        payment.accept(2L, "memo");
        payment.start(2L);

        payment.cancel(2L, "취소");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.CANCELLED);
    }

    @Test
    void cancel_rejects_a_payment_that_is_still_requested() {
        Payment payment = newRequestedPayment();

        assertThatThrownBy(() -> payment.cancel(2L, "reason"))
                .isInstanceOf(ApiException.class);
    }
}
