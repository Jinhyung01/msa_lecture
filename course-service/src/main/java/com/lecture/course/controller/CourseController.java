package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.exception.ForbiddenException;
import com.lecture.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 담당: 백엔드 A
 * 통합 개발 구현 명세서 7.1 외부 API 중 리소스 카탈로그 도메인 + 7.2 내부 API.
 * 역할 값은 기존 STUDENT/INSTRUCTOR를 그대로 사용한다 (Auth Server 소스 없음, 1.4).
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private static final String ADMIN_ROLE = "INSTRUCTOR";

    private final CourseService courseService;

    /** GET /api/courses - 리소스 목록 조회 (API-02) */
    @GetMapping
    public ResponseEntity<List<CourseDto.CourseResponse>> getCourses(
            @RequestParam(required = false) Course.Category category,
            @RequestParam(required = false) Course.Status status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // Sprint 1에서는 page/size를 무시한다 (명세서 8. API-02)
        return ResponseEntity.ok(courseService.getCourses(category, status, keyword));
    }

    /** GET /api/courses/{id} - 리소스 상세 조회 (API-03) */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto.CourseResponse> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    /** POST /api/courses - 리소스 등록 (API-04, 관리자만) */
    @PostMapping
    public ResponseEntity<CourseDto.CourseResponse> createCourse(
            @Valid @RequestBody CourseDto.SaveRequest request,
            @RequestHeader("X-User-Id") Long instructorId,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        CourseDto.CourseResponse response = courseService.createCourse(request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/courses/{id} - 리소스 수정 (API-05, 본인이 등록한 리소스만) */
    @PutMapping("/{id}")
    public ResponseEntity<CourseDto.CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto.SaveRequest request,
            @RequestHeader("X-User-Id") Long instructorId,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(courseService.updateCourse(id, request, instructorId));
    }

    /** PATCH /api/courses/{id}/status - 신청 가능 상태 변경 (관리자만) */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CourseDto.CourseResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto.StatusUpdateRequest request,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(courseService.changeStatus(id, request.getStatus()));
    }

    /** GET /api/courses/internal/exists/{id} - 리소스 존재 여부 (Enrollment Service 호출) */
    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> existsCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.existsCourse(id));
    }

    /** GET /api/courses/internal/{id} - 리소스 상세 (Enrollment/Recommend 내부 호출용, 래퍼 없음) */
    @GetMapping("/internal/{id}")
    public ResponseEntity<CourseDto.CourseResponse> getCourseInternal(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    /** POST /api/courses/internal/{id}/enrollment-count - 제공 완료 횟수 증가 (Enrollment Service 호출) */
    @PostMapping("/internal/{id}/enrollment-count")
    public ResponseEntity<Void> increaseEnrollmentCount(@PathVariable Long id) {
        courseService.increaseEnrollmentCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/courses/internal/recommend - 연관 리소스 후보 조회 (Recommend Service 내부 호출용)
     * categories: 콤마로 구분된 카테고리 목록, excludeIds: 이미 제공/진행 중인 리소스 ID 목록
     */
    @GetMapping("/internal/recommend")
    public ResponseEntity<List<CourseDto.CourseResponse>> getRecommendCandidates(
            @RequestParam List<Course.Category> categories,
            @RequestParam(required = false) List<Long> excludeIds) {
        List<Long> exclude = (excludeIds != null) ? excludeIds : Collections.emptyList();
        return ResponseEntity.ok(courseService.getRecommendCandidates(categories, exclude));
    }

    private void requireAdmin(String role) {
        if (!ADMIN_ROLE.equals(role)) {
            throw new ForbiddenException("이 작업을 수행할 권한이 없습니다");
        }
    }
}
