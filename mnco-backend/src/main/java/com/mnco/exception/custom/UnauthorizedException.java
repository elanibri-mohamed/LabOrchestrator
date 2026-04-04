package com.mnco.exception.custom;

/**
 * Thrown when an authenticated user attempts an action outside their permissions.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
