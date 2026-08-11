package com.lecture.enrollment.controller;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> enroll(
            @Valid @RequestBody EnrollmentDto.EnrollRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        EnrollmentDto.EnrollmentResponse response =
                enrollmentService.enroll(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnrollmentDto.ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.EnrollmentResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Enrollment.Status status) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getEnrollmentsByUser(userId, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> getEnrollment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getEnrollment(id, userId)));
    }

    // PATCH/POST 겸용: api-gateway CORS 허용 메서드 목록에 PATCH가 빠져 있어(소스 없는 사전
    // 빌드 이미지) 브라우저에서 PATCH가 403으로 막힌다. 게이트웨이가 고쳐지기 전까지 프론트는 POST로 호출한다.
    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> cancelEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentDto.CancelRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.cancelByRequester(
                        id, userId, request.getReason())));
    }

    // PATCH/POST 겸용: cancelEnrollment 참고.
    @RequestMapping(value = "/{id}/return", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> returnEnrollment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.returnByRequester(id, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnrollment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        enrollmentService.deleteByRequester(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.EnrollmentResponse>>> getAdminEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Enrollment.Status status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getAdminEnrollments(
                        userId, status, category, keyword)));
    }

    @GetMapping("/internal/history/{userId}")
    public ResponseEntity<EnrollmentDto.EnrollmentHistoryResponse> getEnrollmentHistory(
            @PathVariable Long userId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentHistory(userId));
    }
}
