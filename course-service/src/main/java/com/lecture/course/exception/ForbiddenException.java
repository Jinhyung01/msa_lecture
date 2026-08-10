package com.lecture.course.exception;

import org.springframework.http.HttpStatus;

/** 오류 코드: FORBIDDEN (403) - 권한 또는 소유권 없음 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
