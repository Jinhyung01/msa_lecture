package com.lecture.user.exception;

import org.springframework.http.HttpStatus;

/** 명세서 오류 코드: RESOURCE_NOT_FOUND (404) */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
