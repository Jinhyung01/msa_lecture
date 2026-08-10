package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.exception.ApiException;
import com.lecture.payment.kafka.PaymentKafkaProducer;
import com.lecture.payment.repository.PaymentRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String ADMIN_ROLE = "INSTRUCTOR";

    private final PaymentRepository paymentRepository;
    private final PaymentKafkaProducer kafkaProducer;

    /**
     * API-07: enrollmentId당 제공 작업 1건만 REQUESTED로 생성한다.
     * 기존의 자동 결제/즉시 완료 로직은 만들지 않는다.
     */
    @Transactional
    public PaymentDto.InternalProvisionResponse createProvision(PaymentDto.InternalProvisionRequest request) {
        if (paymentRepository.existsByEnrollmentId(request.getEnrollmentId())) {
            throw ApiException.duplicate(
                    "이미 해당 신청의 제공 작업이 존재합니다: enrollmentId=" + request.getEnrollmentId());
        }

        // ponytail: existsByEnrollmentId 이후 save() 사이에 동시 요청이 끼어들면 DB 유니크 제약 위반이
        // enrollment_id 중복이 아닌 다른 무결성 오류(FK 위반 등)와 구분되지 않고 500으로 나갈 수 있다.
        // Sprint 1은 Enrollment Service가 enrollmentId당 1회만 호출하므로 실질적 경합이 없어 그대로 둔다.
        // 경합이 실제로 발생하면 원인(유니크 제약 위반 SQLState)을 구분해 duplicate()로 매핑한다.
        Payment payment = paymentRepository.save(
                Payment.builder()
                        .enrollmentId(request.getEnrollmentId())
                        .userId(request.getUserId())
                        .courseId(request.getCourseId())
                        .amount(request.getAmount())
                        .build()
        );
        log.info("[PaymentService] 제공 작업 생성 - paymentId: {}, enrollmentId: {}",
                payment.getId(), payment.getEnrollmentId());
        return PaymentDto.InternalProvisionResponse.from(payment);
    }

    public PaymentDto.PaymentResponse getProvision(Long id, Long requesterId, String requesterRole) {
        Payment payment = findOrThrow(id);
        boolean isOwner = payment.getUserId().equals(requesterId);
        boolean isAdmin = ADMIN_ROLE.equals(requesterRole);
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("본인 또는 관리자만 조회할 수 있습니다.");
        }
        return PaymentDto.PaymentResponse.from(payment);
    }

    public List<PaymentDto.PaymentResponse> listAdmin(Payment.Status status) {
        List<Payment> payments = status != null
                ? paymentRepository.findByStatusOrderByCreatedAtDesc(status)
                : paymentRepository.findAllByOrderByCreatedAtDesc();
        return payments.stream().map(PaymentDto.PaymentResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public PaymentDto.PaymentResponse accept(Long id, Long managerId, String managerMemo) {
        Payment payment = findOrThrow(id);
        Payment.Status previous = payment.getStatus();
        payment.accept(managerId, managerMemo);
        paymentRepository.flush();
        kafkaProducer.publishStatusChanged(payment, previous, null);
        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentDto.PaymentResponse start(Long id, Long managerId) {
        Payment payment = findOrThrow(id);
        Payment.Status previous = payment.getStatus();
        payment.start(managerId);
        paymentRepository.flush();
        kafkaProducer.publishStatusChanged(payment, previous, null);
        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentDto.PaymentResponse complete(Long id, Long managerId, String ticketNumber, String resultMemo) {
        Payment payment = findOrThrow(id);
        Payment.Status previous = payment.getStatus();
        payment.complete(managerId, ticketNumber, resultMemo);
        paymentRepository.flush();
        kafkaProducer.publishResourceProvided(payment, previous);
        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentDto.PaymentResponse reject(Long id, Long managerId, String reason) {
        Payment payment = findOrThrow(id);
        Payment.Status previous = payment.getStatus();
        payment.reject(managerId, reason);
        paymentRepository.flush();
        kafkaProducer.publishStatusChanged(payment, previous, reason);
        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentDto.PaymentResponse cancel(Long id, Long managerId, String reason) {
        Payment payment = findOrThrow(id);
        Payment.Status previous = payment.getStatus();
        payment.cancel(managerId, reason);
        paymentRepository.flush();
        kafkaProducer.publishStatusChanged(payment, previous, reason);
        return PaymentDto.PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentDto.PaymentResponse cancelByRequester(Long id, String reason) {
        Payment payment = findOrThrow(id);
        Payment.Status previous = payment.getStatus();
        payment.cancelByRequester(reason);
        paymentRepository.flush();
        kafkaProducer.publishStatusChanged(payment, previous, reason);
        return PaymentDto.PaymentResponse.from(payment);
    }

    private Payment findOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("제공 작업을 찾을 수 없습니다: " + id));
    }
}
