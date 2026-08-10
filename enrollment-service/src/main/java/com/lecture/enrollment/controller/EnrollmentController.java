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

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> cancelEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentDto.CancelRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.cancelByRequester(
                        id, userId, request.getReason())));
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
