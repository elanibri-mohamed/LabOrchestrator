package com.mnco.exception.custom;

/**
 * Thrown when an operation is attempted on a lab in an incompatible state.
 * Example: trying to start a lab that is already running.
 */
public class InvalidLabStateException extends RuntimeException {
    public InvalidLabStateException(String message) {
        super(message);
    }
}
