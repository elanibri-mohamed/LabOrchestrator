package com.mnco.exception.custom;

/**
 * Thrown when a unique constraint would be violated (duplicate username, email, etc.).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
