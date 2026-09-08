package com.example.taskboard.exception;

/**
 * Base class for all application-level exceptions.
 *
 * <p>Subclasses carry a user-friendly message and, where relevant, the
 * underlying cause for internal logging.</p>
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
