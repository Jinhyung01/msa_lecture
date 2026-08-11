package com.lecture.payment.controller;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.exception.ApiException;
import com.lecture.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String ADMIN_ROLE = "INSTRUCTOR";

    private final PaymentService paymentService;

    /**
     * API-07: 내부 생성. Enrollment Service만 호출하며 Gateway 사용자 헤더가 없다.
     */
    @PostMapping("/internal/request")
    public ResponseEntity<PaymentDto.InternalProvisionResponse> createProvision(
            @Valid @RequestBody PaymentDto.InternalProvisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createProvision(request));
    }

    /**
     * 신청자가 REQUESTED 상태의 신청을 취소할 때 Enrollment Service가 호출한다.
     */
    @PatchMapping("/internal/{id}/cancel")
    public ResponseEntity<PaymentDto.PaymentResponse> cancelByRequester(
            @PathVariable Long id,
            @Valid @RequestBody PaymentDto.InternalCancelRequest request) {
        if (!"REQUESTER".equals(request.getRequestedBy())) {
            throw ApiException.forbidden("신청자 취소 요청만 처리할 수 있습니다.");
        }
        return ResponseEntity.ok(paymentService.cancelByRequester(id, request.getReason()));
    }

    /**
     * 신청자가 PROVIDED 상태의 제공을 반납할 때 Enrollment Service가 호출한다 (내부 호출).
     */
    @RequestMapping(value = "/internal/{id}/return", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> returnByRequester(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.returnByRequester(id));
    }

    /**
     * 신청자가 취소·수거완료된 신청 기록을 삭제할 때 Enrollment Service가 호출한다 (내부 호출).
     */
    @DeleteMapping("/internal/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API-12: 관리자 제공 작업 목록
     */
    @GetMapping("/admin")
    public ResponseEntity<List<PaymentDto.PaymentResponse>> listAdmin(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) Payment.Status status) {
        requireAdmin(role);
        return ResponseEntity.ok(paymentService.listAdmin(status));
    }

    /**
     * 제공 작업 상세. 신청자 본인 또는 관리자만 조회 가능.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto.PaymentResponse> getProvision(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(paymentService.getProvision(id, userId, role));
    }

    /**
     * API-13: 관리자 접수
     * PATCH/POST 둘 다 받는다 — api-gateway CORS 허용 메서드 목록에 PATCH가 빠져 있어
     * (SecurityConfig.corsWebFilter, 소스 없는 사전 빌드 이미지) 브라우저에서 PATCH가 403으로 막힌다.
     * 게이트웨이가 고쳐지기 전까지 프론트는 POST로 호출한다.
     */
    @RequestMapping(value = "/{id}/accept", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> accept(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody(required = false) PaymentDto.AcceptRequest request) {
        requireAdmin(role);
        String managerMemo = request != null ? request.getManagerMemo() : null;
        return ResponseEntity.ok(paymentService.accept(id, managerId, managerMemo));
    }

    /**
     * API-14: 제공 시작 (PATCH/POST 겸용 — 사유는 accept 참고)
     */
    @RequestMapping(value = "/{id}/start", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> start(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(paymentService.start(id, managerId));
    }

    /**
     * API-15: 제공 완료 (PATCH/POST 겸용 — 사유는 accept 참고)
     */
    @RequestMapping(value = "/{id}/complete", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> complete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody PaymentDto.CompleteRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(
                paymentService.complete(id, managerId, request.getTicketNumber(), request.getResultMemo()));
    }

    /**
     * API-16: 반려 (PATCH/POST 겸용 — 사유는 accept 참고)
     */
    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> reject(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody PaymentDto.ReasonRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(paymentService.reject(id, managerId, request.getReason()));
    }

    /**
     * API-17: 관리자 제공 취소 (PATCH/POST 겸용 — 사유는 accept 참고)
     */
    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody PaymentDto.ReasonRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(paymentService.cancel(id, managerId, request.getReason()));
    }

    /**
     * API-19: 신청자 취소 신청 승인 (PATCH/POST 겸용 — 사유는 accept 참고)
     */
    @RequestMapping(value = "/{id}/approve-cancel", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> approveCancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(paymentService.approveCancel(id, managerId));
    }

    /**
     * API-18: 관리자 리소스 수거 (PATCH/POST 겸용 — 사유는 accept 참고)
     */
    @RequestMapping(value = "/{id}/collect", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PaymentDto.PaymentResponse> collect(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(paymentService.collect(id, managerId));
    }

    private void requireAdmin(String role) {
        if (!ADMIN_ROLE.equals(role)) {
            throw ApiException.forbidden("이 작업을 수행할 권한이 없습니다.");
        }
    }
}
