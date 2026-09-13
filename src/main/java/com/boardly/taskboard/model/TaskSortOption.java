package com.boardly.taskboard.model;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Sorting options for cards on the Kanban board.
 */
public enum TaskSortOption {
    PRIORITY_DESC("Priority (High to Low)", (c1, c2) -> {
        int cmp = Integer.compare(c2.getPriority().getLevel(), c1.getPriority().getLevel());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    PRIORITY_ASC("Priority (Low to High)", (c1, c2) -> {
        int cmp = Integer.compare(c1.getPriority().getLevel(), c2.getPriority().getLevel());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    SEVERITY_DESC("Severity (High to Low)", (c1, c2) -> {
        int cmp = Integer.compare(c2.getSeverity().getLevel(), c1.getSeverity().getLevel());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    SEVERITY_ASC("Severity (Low to High)", (c1, c2) -> {
        int cmp = Integer.compare(c1.getSeverity().getLevel(), c2.getSeverity().getLevel());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    DUE_DATE_ASC("Due Date (Earliest First)", (c1, c2) -> {
        LocalDate d1 = c1.getDueDate();
        LocalDate d2 = c2.getDueDate();
        if (d1 == null && d2 == null)
            return Double.compare(c1.getPosition(), c2.getPosition());
        if (d1 == null)
            return 1;
        if (d2 == null)
            return -1;
        int cmp = d1.compareTo(d2);
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    DUE_DATE_DESC("Due Date (Latest First)", (c1, c2) -> {
        LocalDate d1 = c1.getDueDate();
        LocalDate d2 = c2.getDueDate();
        if (d1 == null && d2 == null)
            return Double.compare(c1.getPosition(), c2.getPosition());
        if (d1 == null)
            return 1;
        if (d2 == null)
            return -1;
        int cmp = d2.compareTo(d1);
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    CREATED_AT_DESC("Created On (Newest First)", (c1, c2) -> {
        int cmp = c2.getCreatedAt().compareTo(c1.getCreatedAt());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    CREATED_AT_ASC("Created On (Oldest First)", (c1, c2) -> {
        int cmp = c1.getCreatedAt().compareTo(c2.getCreatedAt());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    NAME_ASC("Name (A to Z)", (c1, c2) -> {
        int cmp = c1.getTitle().compareToIgnoreCase(c2.getTitle());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    }),
    NAME_DESC("Name (Z to A)", (c1, c2) -> {
        int cmp = c2.getTitle().compareToIgnoreCase(c1.getTitle());
        if (cmp != 0)
            return cmp;
        return Double.compare(c1.getPosition(), c2.getPosition());
    });

    private final String displayName;
    private final Comparator<Card> comparator;

    TaskSortOption(String displayName, Comparator<Card> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Comparator<Card> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
