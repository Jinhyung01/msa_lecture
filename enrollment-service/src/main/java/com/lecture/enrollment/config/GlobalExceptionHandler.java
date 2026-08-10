package com.lecture.enrollment.config;

import com.lecture.enrollment.dto.ApiErrorResponse;
import com.lecture.enrollment.exception.EnrollmentApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EnrollmentApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            EnrollmentApiException e,
            HttpServletRequest request) {
        return build(e.getStatus(), e.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "요청 파라미터 형식이 올바르지 않습니다: " + e.getName(),
                request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(
            MissingRequestHeaderException e,
            HttpServletRequest request) {
        return build(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "인증 사용자 정보가 없습니다: " + e.getHeaderName(),
                request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException e,
            HttpServletRequest request) {
        return build(
                HttpStatus.CONFLICT,
                "INVALID_STATUS_TRANSITION",
                e.getMessage(),
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                e.getMessage(),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception e,
            HttpServletRequest request) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "서버 오류가 발생했습니다.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .build());
    }
}
