package com.boardly.taskboard.model;

/**
 * Severity levels for a task card.
 */
public enum CardSeverity {
    LOW("Low", 1),
    MINOR("Minor", 2),
    MAJOR("Major", 3),
    CRITICAL("Critical", 4),
    BLOCKER("Blocker", 5);

    private final String displayName;
    private final int level;

    CardSeverity(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public static CardSeverity fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MINOR;
        }
        for (CardSeverity s : values()) {
            if (s.name().equalsIgnoreCase(code.trim()) || s.displayName.equalsIgnoreCase(code.trim())) {
                return s;
            }
        }
        return MINOR;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
