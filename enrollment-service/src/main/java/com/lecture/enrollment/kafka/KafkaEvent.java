package com.lecture.enrollment.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class KafkaEvent {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProvisionStatusEvent {
        private String eventId;
        private String eventType;
        private String occurredAt;
        private Long provisionId;
        private Long enrollmentId;
        private Long userId;
        private Long resourceId;
        private Long managerId;
        private String previousStatus;
        private String status;
        private String reason;
        private String ticketNumber;
        private String resultMemo;
    }
}
