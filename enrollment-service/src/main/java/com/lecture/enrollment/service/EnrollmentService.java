package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.exception.EnrollmentApiException;
import com.lecture.enrollment.kafka.KafkaEvent;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private static final Set<Enrollment.Status> DUPLICATE_BLOCKING_STATUSES = EnumSet.of(
            Enrollment.Status.REQUESTED,
            Enrollment.Status.ACCEPTED,
            Enrollment.Status.PROVISIONING,
            Enrollment.Status.PROVIDED
    );

    private final EnrollmentRepository enrollmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final EnrollmentWriteService enrollmentWriteService;
    private final UserServiceClient userServiceClient;

    public EnrollmentDto.EnrollmentResponse enroll(
            Long userId,
            EnrollmentDto.EnrollRequest request) {

        requireRequester(userId);

        Map<String, Object> courseInfo = loadActiveCourse(request.getCourseId());

        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatusIn(
                userId,
                request.getCourseId(),
                List.copyOf(DUPLICATE_BLOCKING_STATUSES))) {
            throw EnrollmentApiException.duplicateRequest();
        }

        Enrollment enrollment = enrollmentWriteService.createPendingEnrollment(userId, request);

        try {
            PaymentServiceClient.PaymentResult provision = paymentServiceClient.requestPayment(
                    enrollment.getId(),
                    userId,
                    request.getCourseId(),
                    toBigDecimal(courseInfo.get("price"))
            );
            Enrollment attached = enrollmentWriteService.attachPayment(
                    enrollment.getId(), provision.getPaymentId());

            log.info("[EnrollmentService] 리소스 신청 완료 - enrollmentId: {}, paymentId: {}",
                    attached.getId(), attached.getPaymentId());
            return EnrollmentDto.EnrollmentResponse.from(attached, toCourseSummary(courseInfo));
        } catch (RuntimeException e) {
            enrollmentWriteService.markProvisionCreationFailed(
                    enrollment.getId(), "제공 작업 생성 실패");
            throw e;
        }
    }

    public List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByUser(
            Long userId,
            Enrollment.Status status) {
        List<Enrollment> enrollments = status == null
                ? enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : enrollmentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return enrich(enrollments);
    }

    public List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByUser(Long userId) {
        return getEnrollmentsByUser(userId, null);
    }

    public EnrollmentDto.EnrollmentResponse getEnrollment(
            Long enrollmentId,
            Long userId) {
        Enrollment enrollment = findEnrollment(enrollmentId);
        if (!enrollment.getUserId().equals(userId) && !isAdmin(userId)) {
            throw EnrollmentApiException.forbidden("본인의 신청만 조회할 수 있습니다.");
        }
        return EnrollmentDto.EnrollmentResponse.from(
                enrollment,
                toCourseSummary(courseServiceClient.getCourse(enrollment.getCourseId())));
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse cancelByRequester(
            Long enrollmentId,
            Long userId,
            String reason) {
        requireRequester(userId);

        Enrollment enrollment = findEnrollment(enrollmentId);
        if (!enrollment.getUserId().equals(userId)) {
            throw EnrollmentApiException.forbidden("본인의 신청만 취소할 수 있습니다.");
        }
        if (enrollment.getStatus() != Enrollment.Status.REQUESTED) {
            throw EnrollmentApiException.invalidTransition(
                    "REQUESTED 상태의 신청만 취소할 수 있습니다.");
        }
        if (enrollment.getPaymentId() == null) {
            throw EnrollmentApiException.internalError("연결된 제공 작업이 없습니다.");
        }

        enrollment.cancelByRequester(reason.trim());
        paymentServiceClient.cancelProvision(enrollment.getPaymentId(), reason.trim());

        log.info("[EnrollmentService] 신청자 취소 - enrollmentId: {}, userId: {}",
                enrollmentId, userId);
        return EnrollmentDto.EnrollmentResponse.from(
                enrollment,
                toCourseSummary(courseServiceClient.getCourse(enrollment.getCourseId())));
    }

    public List<EnrollmentDto.EnrollmentResponse> getAdminEnrollments(
            Long userId,
            Enrollment.Status status,
            String category,
            String keyword) {
        requireAdmin(userId);

        List<Enrollment> enrollments = status == null
                ? enrollmentRepository.findAllByOrderByCreatedAtDesc()
                : enrollmentRepository.findByStatusOrderByCreatedAtDesc(status);

        String normalizedCategory = trimToNull(category);
        String normalizedKeyword = trimToNull(keyword);

        return enrich(enrollments).stream()
                .filter(item -> normalizedCategory == null
                        || (item.getCourse() != null
                        && normalizedCategory.equalsIgnoreCase(item.getCourse().getCategory())))
                .filter(item -> normalizedKeyword == null
                        || containsIgnoreCase(String.valueOf(item.getUserId()), normalizedKeyword)
                        || (item.getCourse() != null
                        && containsIgnoreCase(item.getCourse().getTitle(), normalizedKeyword)))
                .toList();
    }

    public EnrollmentDto.EnrollmentHistoryResponse getEnrollmentHistory(Long userId) {
        List<Long> providedCourseIds = enrollmentRepository
                .findByUserIdAndStatus(userId, Enrollment.Status.PROVIDED)
                .stream()
                .map(Enrollment::getCourseId)
                .distinct()
                .toList();

        return EnrollmentDto.EnrollmentHistoryResponse.builder()
                .userId(userId)
                .providedCourseIds(providedCourseIds)
                .activeCourseIds(providedCourseIds)
                .build();
    }

    @Transactional
    public boolean applyProvisionEvent(KafkaEvent.ProvisionStatusEvent event) {
        Enrollment enrollment = enrollmentRepository.findById(event.getEnrollmentId())
                .orElseThrow(() -> EnrollmentApiException.requestNotFound(event.getEnrollmentId()));

        if (event.getProvisionId() != null
                && enrollment.getPaymentId() != null
                && !event.getProvisionId().equals(enrollment.getPaymentId())) {
            throw EnrollmentApiException.invalidTransition(
                    "이벤트의 제공 작업 ID가 신청과 일치하지 않습니다.");
        }

        Enrollment.Status previousStatus = parseStatus(event.getPreviousStatus());
        Enrollment.Status nextStatus = parseRequiredStatus(event.getStatus());

        final boolean changed;
        try {
            changed = enrollment.applyProvisionStatus(
                    event.getEventId(), previousStatus, nextStatus, event.getReason());
        } catch (IllegalStateException e) {
            throw EnrollmentApiException.invalidTransition(e.getMessage());
        }

        if (changed && nextStatus == Enrollment.Status.PROVIDED) {
            courseServiceClient.increaseEnrollmentCount(enrollment.getCourseId());
        }

        log.info("[EnrollmentService] 제공 상태 이벤트 적용 - enrollmentId: {}, status: {}, changed: {}",
                enrollment.getId(), nextStatus, changed);
        return changed;
    }

    private List<EnrollmentDto.EnrollmentResponse> enrich(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(enrollment -> EnrollmentDto.EnrollmentResponse.from(
                        enrollment,
                        toCourseSummary(courseServiceClient.getCourse(enrollment.getCourseId()))))
                .toList();
    }

    private Map<String, Object> loadActiveCourse(Long courseId) {
        if (!courseServiceClient.existsCourse(courseId)) {
            throw EnrollmentApiException.resourceNotFound(courseId);
        }
        Map<String, Object> courseInfo = courseServiceClient.getCourse(courseId);
        String status = Objects.toString(courseInfo.get("status"), "ACTIVE");
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw EnrollmentApiException.badRequest("현재 신청할 수 없는 리소스입니다.");
        }
        return courseInfo;
    }

    private Enrollment findEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> EnrollmentApiException.requestNotFound(enrollmentId));
    }

    private EnrollmentDto.CourseSummary toCourseSummary(Map<String, Object> courseInfo) {
        return EnrollmentDto.CourseSummary.builder()
                .id(toLong(courseInfo.get("id")))
                .title(Objects.toString(courseInfo.get("title"), null))
                .description(Objects.toString(courseInfo.get("description"), null))
                .category(Objects.toString(courseInfo.get("category"), null))
                .price(toBigDecimal(courseInfo.get("price")))
                .thumbnail(Objects.toString(courseInfo.get("thumbnail"), null))
                .instructorName(firstNonBlank(
                        Objects.toString(courseInfo.get("instructorName"), null),
                        Objects.toString(courseInfo.get("teacherName"), null),
                        Objects.toString(courseInfo.get("instructor_name"), null)))
                .enrollmentCount(toInteger(firstNonNull(
                        courseInfo.get("enrollmentCount"),
                        courseInfo.get("enrollment_count"))))
                .status(Objects.toString(courseInfo.get("status"), null))
                .build();
    }

    private void requireRequester(Long userId) {
        String role = userServiceClient.getRole(userId);
        if (!"STUDENT".equalsIgnoreCase(role)) {
            throw EnrollmentApiException.forbidden("신청자만 리소스를 신청하거나 취소할 수 있습니다.");
        }
    }

    private void requireAdmin(Long userId) {
        if (!isAdmin(userId)) {
            throw EnrollmentApiException.forbidden("리소스 관리자만 전체 신청을 조회할 수 있습니다.");
        }
    }

    private boolean isAdmin(Long userId) {
        return "INSTRUCTOR".equalsIgnoreCase(userServiceClient.getRole(userId));
    }

    private Enrollment.Status parseRequiredStatus(String status) {
        Enrollment.Status parsed = parseStatus(status);
        if (parsed == null) {
            throw EnrollmentApiException.badRequest("Kafka 이벤트 상태는 필수입니다.");
        }
        return parsed;
    }

    private Enrollment.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Enrollment.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw EnrollmentApiException.badRequest("알 수 없는 신청 상태입니다: " + status);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(value.toString());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }
}
