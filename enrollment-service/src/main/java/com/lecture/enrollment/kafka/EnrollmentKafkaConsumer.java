package com.lecture.enrollment.kafka;

import com.lecture.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentKafkaConsumer {

    private final EnrollmentService enrollmentService;

    @KafkaListener(
            topics = "${kafka.topic.provision-status-changed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProvisionStatusChanged(Map<String, Object> rawEvent) {
        handle("provision.status-changed", rawEvent);
    }

    @KafkaListener(
            topics = "${kafka.topic.resource-provided}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleResourceProvided(Map<String, Object> rawEvent) {
        handle("resource.provided", rawEvent);
    }

    private void handle(String topic, Map<String, Object> rawEvent) {
        log.info("[Kafka Consumer] {} 이벤트 수신: {}", topic, rawEvent);
        try {
            KafkaEvent.ProvisionStatusEvent event = toEvent(rawEvent);
            validate(event);
            boolean changed = enrollmentService.applyProvisionEvent(event);
            log.info("[Kafka Consumer] {} 처리 완료 - eventId: {}, enrollmentId: {}, changed: {}",
                    topic, event.getEventId(), event.getEnrollmentId(), changed);
        } catch (Exception e) {
            log.error("[Kafka Consumer] {} 처리 실패 - event: {}, error: {}",
                    topic, rawEvent, e.getMessage(), e);
            throw new IllegalStateException(topic + " 이벤트 처리 실패", e);
        }
    }

    private KafkaEvent.ProvisionStatusEvent toEvent(Map<String, Object> event) {
        return KafkaEvent.ProvisionStatusEvent.builder()
                .eventId(toStringValue(event.get("eventId")))
                .eventType(toStringValue(event.get("eventType")))
                .occurredAt(toStringValue(event.get("occurredAt")))
                .provisionId(toLong(firstNonNull(
                        event.get("provisionId"), event.get("paymentId"))))
                .enrollmentId(toLong(event.get("enrollmentId")))
                .userId(toLong(event.get("userId")))
                .resourceId(toLong(firstNonNull(
                        event.get("resourceId"), event.get("courseId"))))
                .managerId(toLong(event.get("managerId")))
                .previousStatus(toStringValue(event.get("previousStatus")))
                .status(toStringValue(event.get("status")))
                .reason(toStringValue(event.get("reason")))
                .ticketNumber(toStringValue(event.get("ticketNumber")))
                .resultMemo(toStringValue(event.get("resultMemo")))
                .build();
    }

    private void validate(KafkaEvent.ProvisionStatusEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("eventId는 필수입니다.");
        }
        if (event.getEnrollmentId() == null) {
            throw new IllegalArgumentException("enrollmentId는 필수입니다.");
        }
        if (event.getStatus() == null || event.getStatus().isBlank()) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
