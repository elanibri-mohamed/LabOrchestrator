package com.mnco.exception.custom;

/**
 * Thrown when communication with the EVE-NG API fails.
 */
public class EveNgIntegrationException extends RuntimeException {
    public EveNgIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
    public EveNgIntegrationException(String message) {
        super(message);
    }
}
