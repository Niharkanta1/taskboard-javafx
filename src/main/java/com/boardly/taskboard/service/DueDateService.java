package com.boardly.taskboard.service;

import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.model.DueDateStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Computes the display status, display text and CSS class for a card's due date.
 *
 * <p>Rules:
 * <ul>
 *   <li>no due date → {@link DueDateStatus#NONE}</li>
 *   <li>future date → {@link DueDateStatus#UPCOMING}</li>
 *   <li>today → {@link DueDateStatus#DUE_TODAY}</li>
 *   <li>past date → {@link DueDateStatus#OVERDUE}</li>
 *   <li>final card status (COMPLETED or CLOSED) → {@link DueDateStatus#COMPLETED},
 *       so completed/closed cards are never shown as overdue.</li>
 * </ul>
 *
 * <p>Due dates are date-only ({@link LocalDate}); there is no time component,
 * so a due date equal to today is DUE_TODAY at any time of day, including midnight.</p>
 */
public class DueDateService {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    /**
     * Determines the due-date status using the current date.
     */
    public DueDateStatus status(LocalDate dueDate, CardStatus cardStatus) {
        return status(dueDate, cardStatus, LocalDate.now());
    }

    /**
     * Determines the due-date status relative to an explicit reference date.
     *
     * <p>The reference date is exposed for deterministic boundary tests
     * (today, midnight, tomorrow, yesterday).</p>
     */
    public DueDateStatus status(LocalDate dueDate, CardStatus cardStatus, LocalDate today) {
        if (dueDate == null) {
            return DueDateStatus.NONE;
        }
        if (cardStatus == CardStatus.COMPLETED || cardStatus == CardStatus.CLOSED) {
            return DueDateStatus.COMPLETED;
        }
        if (dueDate.isBefore(today)) {
            return DueDateStatus.OVERDUE;
        }
        if (dueDate.isAfter(today)) {
            return DueDateStatus.UPCOMING;
        }
        return DueDateStatus.DUE_TODAY;
    }

    /**
     * Human-readable text for the due-date status, e.g. "Due Sep 12",
     * "Due today", "Overdue" or "Completed". Empty for {@code NONE}.
     */
    public String displayText(DueDateStatus status, LocalDate dueDate) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case UPCOMING:
                return "Due " + format(dueDate);
            case DUE_TODAY:
                return "Due today";
            case OVERDUE:
                return "Overdue";
            case COMPLETED:
                return "Completed";
            case NONE:
            default:
                return "";
        }
    }

    /**
     * CSS style class for the due-date label: {@code due-normal},
     * {@code due-today}, {@code due-overdue} or {@code due-completed}.
     *
     * @return {@code null} when no due-date label should be shown.
     */
    public String styleClass(DueDateStatus status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case UPCOMING:
                return "due-normal";
            case DUE_TODAY:
                return "due-today";
            case OVERDUE:
                return "due-overdue";
            case COMPLETED:
                return "due-completed";
            case NONE:
            default:
                return null;
        }
    }

    private String format(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }
}
