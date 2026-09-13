package com.boardly.taskboard.model;

/**
 * Priority levels for a task card.
 */
public enum CardPriority {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    private final String displayName;
    private final int level;

    CardPriority(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public static CardPriority fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MEDIUM;
        }
        for (CardPriority p : values()) {
            if (p.name().equalsIgnoreCase(code.trim()) || p.displayName.equalsIgnoreCase(code.trim())) {
                return p;
            }
        }
        return MEDIUM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
