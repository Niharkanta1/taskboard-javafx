package com.boardly.taskboard.exception;

/**
 * Thrown when authentication fails (unknown user or incorrect password).
 *
 * <p>The message is safe to display in the UI: it does not reveal which
 * part of the credentials was wrong.</p>
 */
public class AuthenticationException extends AppException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
