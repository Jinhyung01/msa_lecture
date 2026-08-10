package com.lecture.enrollment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "enrollments", indexes = {
        @Index(name = "idx_enrollment_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_enrollment_status", columnList = "status"),
        @Index(name = "idx_enrollment_payment", columnList = "payment_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "payment_id", unique = true)
    private Long paymentId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "desired_date")
    private LocalDate desiredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.REQUESTED;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "last_event_id", length = 100)
    private String lastEventId;

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

    public void attachPayment(Long paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("제공 작업 ID는 필수입니다.");
        }
        if (this.paymentId != null && !this.paymentId.equals(paymentId)) {
            throw new IllegalStateException("이미 다른 제공 작업이 연결되어 있습니다.");
        }
        this.paymentId = paymentId;
    }

    public void cancelByRequester(String reason) {
        if (this.status != Status.REQUESTED) {
            throw new IllegalStateException("REQUESTED 상태의 신청만 취소할 수 있습니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
    }

    /**
     * Payment Service에서 전달된 상태 이벤트를 적용한다.
     *
     * @return 실제 상태가 변경되었으면 true, 중복 이벤트면 false
     */
    public boolean applyProvisionStatus(
            String eventId,
            Status previousStatus,
            Status nextStatus,
            String eventReason) {

        if (eventId != null && eventId.equals(this.lastEventId)) {
            return false;
        }
        if (this.status == nextStatus) {
            this.lastEventId = eventId;
            return false;
        }
        if (previousStatus != null && this.status != previousStatus) {
            throw new IllegalStateException(
                    "신청 상태가 이벤트의 이전 상태와 일치하지 않습니다. current="
                            + this.status + ", eventPrevious=" + previousStatus);
        }
        if (!isAllowedTransition(this.status, nextStatus)) {
            throw new IllegalStateException(
                    "허용되지 않은 신청 상태 전이입니다: " + this.status + " -> " + nextStatus);
        }

        this.status = nextStatus;
        this.lastEventId = eventId;
        if (nextStatus == Status.REJECTED) {
            this.rejectReason = eventReason;
        }
        if (nextStatus == Status.CANCELLED) {
            this.cancelReason = eventReason;
        }
        return true;
    }

    public void cancelAfterProvisionCreationFailure(String reason) {
        if (this.status == Status.REQUESTED) {
            this.status = Status.CANCELLED;
            this.cancelReason = reason;
        }
    }

    private boolean isAllowedTransition(Status current, Status next) {
        return switch (current) {
            case REQUESTED -> next == Status.ACCEPTED
                    || next == Status.REJECTED
                    || next == Status.CANCELLED;
            case ACCEPTED -> next == Status.PROVISIONING
                    || next == Status.CANCELLED;
            case PROVISIONING -> next == Status.PROVIDED
                    || next == Status.CANCELLED;
            case PROVIDED, REJECTED, CANCELLED -> false;
        };
    }
}
