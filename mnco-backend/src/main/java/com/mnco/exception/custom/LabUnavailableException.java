package com.mnco.exception.custom;

/**
 * Thrown when a lab template is unavailable for assignment
 * (e.g., template status is REMOVED or not found).
 */
public class LabUnavailableException extends RuntimeException {
    public LabUnavailableException(String message) {
        super(message);
    }
}
