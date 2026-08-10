package com.lecture.user.controller;

import com.lecture.user.dto.UserDto;
import com.lecture.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 담당: 백엔드 A
 * 통합 개발 구현 명세서 7.1 외부 API 중 사용자 도메인:
 *   GET /api/users/me - 로그인 사용자 내 정보 (Gateway X-User-Id 헤더 사용)
 * 그 외 register/{id}는 기존 스켈레톤 기능을 유지한다 (역할·구조 변경 없음).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /api/users/register - 사번 기반 회원가입 (공개)
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto.UserResponse> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {
        UserDto.UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/users/me - 로그인 사용자 내 정보 조회 (API-01)
     * Gateway가 전달한 X-User-Id 헤더 사용. 헤더가 없으면 GlobalExceptionHandler가 401을 반환한다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto.UserResponse> getMe(
            @RequestHeader("X-User-Id") Long userId) {
        UserDto.UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/{id} - 사용자 조회 (관리자용, 공통 인증)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto.UserResponse> getUser(@PathVariable Long id) {
        UserDto.UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/internal/{id} - 서비스 간 내부 호출용 (Enrollment/Payment 등에서 사용자 정보 조회)
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<UserDto.UserResponse> getUserInternal(@PathVariable Long id) {
        UserDto.UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}
