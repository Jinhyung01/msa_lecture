package com.lecture.user.config;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 통합 개발 구현 명세서 6.4 공통 오류 응답 포맷
 * {
 *   "timestamp": "...",
 *   "status": 409,
 *   "code": "INVALID_STATUS_TRANSITION",
 *   "message": "...",
 *   "path": "/api/..."
 * }
 */
@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private String path;

    public static ErrorResponse of(int status, String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .build();
    }
}
