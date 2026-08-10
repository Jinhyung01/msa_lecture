package com.lecture.payment.kafka;

import com.lecture.payment.entity.Payment;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.provision-status-changed}")
    private String statusChangedTopic;

    @Value("${kafka.topic.resource-provided}")
    private String resourceProvidedTopic;

    /**
     * provision.status-changed 발행 (접수/시작/반려/취소 시 호출)
     */
    public void publishStatusChanged(Payment payment, Payment.Status previousStatus, String reason) {
        ProvisionStatusChangedEvent event = ProvisionStatusChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PROVISION_STATUS_CHANGED")
                .occurredAt(LocalDateTime.now().toString())
                .provisionId(payment.getId())
                .enrollmentId(payment.getEnrollmentId())
                .userId(payment.getUserId())
                .resourceId(payment.getCourseId())
                .managerId(payment.getManagerId())
                .previousStatus(previousStatus.name())
                .status(payment.getStatus().name())
                .reason(reason)
                .build();

        log.info("[Kafka Producer] provision.status-changed 발행 시도 - provisionId: {}, {} -> {}",
                payment.getId(), previousStatus, payment.getStatus());

        send(statusChangedTopic, String.valueOf(payment.getEnrollmentId()), event, "provision.status-changed");
    }

    /**
     * resource.provided 발행 (제공 완료 시에만 호출)
     */
    public void publishResourceProvided(Payment payment, Payment.Status previousStatus) {
        ResourceProvidedEvent event = ResourceProvidedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("RESOURCE_PROVIDED")
                .occurredAt(LocalDateTime.now().toString())
                .provisionId(payment.getId())
                .enrollmentId(payment.getEnrollmentId())
                .userId(payment.getUserId())
                .resourceId(payment.getCourseId())
                .managerId(payment.getManagerId())
                .previousStatus(previousStatus.name())
                .status(payment.getStatus().name())
                .ticketNumber(payment.getTransactionId())
                .resultMemo(payment.getResultMemo())
                .build();

        log.info("[Kafka Producer] resource.provided 발행 시도 - provisionId: {}", payment.getId());

        send(resourceProvidedTopic, String.valueOf(payment.getEnrollmentId()), event, "resource.provided");
    }

    private void send(String topic, String key, Object event, String label) {
        try {
            kafkaTemplate.send(topic, key, event).get(10, TimeUnit.SECONDS);
            log.info("[Kafka Producer] {} 발행 성공 - topic: {}", label, topic);
        } catch (Exception e) {
            log.error("[Kafka Producer] {} 발행 실패 - topic: {}, error: {}", label, topic, e.getMessage(), e);
            throw new IllegalStateException(label + " Kafka 발행 실패", e);
        }
    }

    @Getter
    @Builder
    public static class ProvisionStatusChangedEvent {
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
    }

    @Getter
    @Builder
    public static class ResourceProvidedEvent {
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
        private String ticketNumber;
        private String resultMemo;
    }
}
