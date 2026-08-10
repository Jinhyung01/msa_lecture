package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.exception.EnrollmentApiException;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentWriteService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * 반드시 독립 트랜잭션으로 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Enrollment createPendingEnrollment(
            Long userId,
            EnrollmentDto.EnrollRequest request) {

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.builder()
                        .userId(userId)
                        .courseId(request.getCourseId())
                        .reason(request.getReason().trim())
                        .quantity(request.getQuantity())
                        .desiredDate(request.getDesiredDate())
                        .status(Enrollment.Status.REQUESTED)
                        .build()
        );

        log.info("[EnrollmentWriteService] REQUESTED 신청 생성 - enrollmentId: {}, userId: {}, courseId: {}",
                enrollment.getId(), userId, request.getCourseId());

        return enrollment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Enrollment attachPayment(Long enrollmentId, Long paymentId) {
        Enrollment enrollment = findById(enrollmentId);
        enrollment.attachPayment(paymentId);
        log.info("[EnrollmentWriteService] 제공 작업 연결 - enrollmentId: {}, paymentId: {}",
                enrollmentId, paymentId);
        return enrollment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProvisionCreationFailed(Long enrollmentId, String reason) {
        Enrollment enrollment = findById(enrollmentId);
        enrollment.cancelAfterProvisionCreationFailure(reason);
        log.warn("[EnrollmentWriteService] 제공 작업 생성 실패로 신청 취소 - enrollmentId: {}",
                enrollmentId);
    }

    private Enrollment findById(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> EnrollmentApiException.requestNotFound(enrollmentId));
    }
}
