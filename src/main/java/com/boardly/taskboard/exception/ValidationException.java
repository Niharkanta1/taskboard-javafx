package com.boardly.taskboard.exception;

/**
 * Thrown when user input does not satisfy the validation rules.
 *
 * <p>The message is intended to be shown directly to the user.</p>
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message);
    }
}
