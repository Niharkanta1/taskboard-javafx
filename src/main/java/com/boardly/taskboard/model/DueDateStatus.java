package com.boardly.taskboard.model;

/**
 * Derived display status of a card's due date.
 *
 * <p>Rules:
 * <ul>
 *   <li>{@code NONE} — the card has no due date.</li>
 *   <li>{@code UPCOMING} — the due date is in the future.</li>
 *   <li>{@code DUE_TODAY} — the due date is today.</li>
 *   <li>{@code OVERDUE} — the due date is in the past.</li>
 *   <li>{@code COMPLETED} — the card is in a final status (COMPLETED or CLOSED);
 *       its due date is not treated as overdue.</li>
 * </ul>
 */
public enum DueDateStatus {
    NONE,
    UPCOMING,
    DUE_TODAY,
    OVERDUE,
    COMPLETED
}
