package com.mnco.exception.custom;

/**
 * Thrown on authentication failure (bad password or unknown user).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
