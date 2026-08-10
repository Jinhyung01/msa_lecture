package com.lecture.user.exception;

import org.springframework.http.HttpStatus;

/**
 * 통합 개발 구현 명세서 6.4 공통 오류 응답 규칙을 따르는 예외 기반 클래스.
 * status(HTTP 상태) + code(오류 코드) + message(사용자 메시지) 조합으로
 * GlobalExceptionHandler가 공통 오류 포맷으로 변환한다.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
