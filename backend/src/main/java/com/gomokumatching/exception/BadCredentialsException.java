package com.gomokumatching.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 */
public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException(String message) {
        super(message);
    }

    public BadCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
