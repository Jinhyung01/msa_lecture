package com.lecture.payment.entity;

import com.lecture.payment.exception.ApiException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private Long enrollmentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.REQUESTED;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "manager_memo", length = 500)
    private String managerMemo;

    @Column(name = "result_memo", length = 1000)
    private String resultMemo;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "provided_at")
    private LocalDateTime providedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        REQUESTED,
        ACCEPTED,
        PROVISIONING,
        PROVIDED,
        REJECTED,
        CANCELLED
    }

    public void accept(Long managerId, String managerMemo) {
        requireStatus(Status.REQUESTED, "REQUESTED 상태에서만 접수할 수 있습니다.");
        this.status = Status.ACCEPTED;
        this.managerId = managerId;
        this.managerMemo = managerMemo;
    }

    public void start(Long managerId) {
        requireStatus(Status.ACCEPTED, "ACCEPTED 상태에서만 제공을 시작할 수 있습니다.");
        this.status = Status.PROVISIONING;
        this.managerId = managerId;
    }

    public void complete(Long managerId, String ticketNumber, String resultMemo) {
        requireStatus(Status.PROVISIONING, "PROVISIONING 상태에서만 제공을 완료할 수 있습니다.");
        this.status = Status.PROVIDED;
        this.managerId = managerId;
        this.transactionId = ticketNumber;
        this.resultMemo = resultMemo;
        this.providedAt = LocalDateTime.now();
    }

    public void reject(Long managerId, String reason) {
        requireStatus(Status.REQUESTED, "REQUESTED 상태에서만 반려할 수 있습니다.");
        this.status = Status.REJECTED;
        this.managerId = managerId;
        this.rejectReason = reason;
    }

    public void cancel(Long managerId, String reason) {
        if (status != Status.ACCEPTED && status != Status.PROVISIONING) {
            throw ApiException.invalidTransition("ACCEPTED 또는 PROVISIONING 상태에서만 취소할 수 있습니다.");
        }
        this.status = Status.CANCELLED;
        this.managerId = managerId;
        this.cancelReason = reason;
    }

    public void cancelByRequester(String reason) {
        requireStatus(Status.REQUESTED, "REQUESTED 상태에서만 신청자가 취소할 수 있습니다.");
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
    }

    private void requireStatus(Status expected, String message) {
        if (this.status != expected) {
            throw ApiException.invalidTransition(message);
        }
    }
}
