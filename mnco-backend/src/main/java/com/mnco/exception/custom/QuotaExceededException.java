package com.mnco.exception.custom;

/**
 * Thrown when a user's resource quota (lab count, CPU, RAM) would be exceeded.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
