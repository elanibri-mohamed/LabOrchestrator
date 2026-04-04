package com.mnco.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Structured error payload returned by the global exception handler.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null);
    }

    public static ErrorResponse ofValidation(int status, String message,
                                              String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(status, "Validation Failed", message, path, Instant.now(), fieldErrors);
    }
}
