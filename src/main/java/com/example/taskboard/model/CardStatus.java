package com.example.taskboard.model;

/**
 * Card lifecycle status.
 *
 * <p>Persisted as a stable string value, never as an ordinal number.</p>
 */
public enum CardStatus {

    PLANNED("PLANNED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CLOSED("CLOSED");

    private final String code;

    CardStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CardStatus fromCode(String code) {
        if (code != null) {
            for (CardStatus status : values()) {
                if (status.code.equalsIgnoreCase(code)) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown card status: " + code);
    }
}
