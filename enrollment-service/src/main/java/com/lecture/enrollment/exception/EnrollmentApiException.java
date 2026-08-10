package com.lecture.enrollment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EnrollmentApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public EnrollmentApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static EnrollmentApiException badRequest(String message) {
        return new EnrollmentApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public static EnrollmentApiException forbidden(String message) {
        return new EnrollmentApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static EnrollmentApiException requestNotFound(Long id) {
        return new EnrollmentApiException(
                HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "신청 정보를 찾을 수 없습니다: " + id);
    }

    public static EnrollmentApiException resourceNotFound(Long id) {
        return new EnrollmentApiException(
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다: " + id);
    }

    public static EnrollmentApiException duplicateRequest() {
        return new EnrollmentApiException(
                HttpStatus.CONFLICT,
                "DUPLICATE_REQUEST",
                "이미 신청 중이거나 제공받은 리소스입니다.");
    }

    public static EnrollmentApiException invalidTransition(String message) {
        return new EnrollmentApiException(
                HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", message);
    }

    public static EnrollmentApiException internalError(String message) {
        return new EnrollmentApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message);
    }
}
