package com.boardly.taskboard.exception;

/**
 * Indicates a failure while accessing the database.
 */
public class DatabaseException extends AppException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
