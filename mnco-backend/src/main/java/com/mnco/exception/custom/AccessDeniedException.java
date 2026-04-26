package com.mnco.exception.custom;

import org.springframework.security.access.AccessDeniedException;

/**
 * Custom AccessDeniedException that extends Spring's AccessDeniedException
 * to allow for consistent handling in the GlobalExceptionHandler.
 */
public class AccessDeniedException extends org.springframework.security.access.AccessDeniedException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}