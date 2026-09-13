package com.boardly.taskboard.service;

import com.boardly.taskboard.model.DueDateStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for DueDateService covering date boundaries (today at midnight,
 * today, tomorrow, yesterday) and status transitions. A fixed reference date
 * keeps the tests deterministic.
 */
class DueDateServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

    private DueDateService service;

    @BeforeEach
    void setUp() {
        service = new DueDateService();
    }

    @Test
    void noDueDateIsNoneForEveryStatus() {
        assertEquals(DueDateStatus.NONE, service.status(null, false, TODAY));
        assertEquals(DueDateStatus.NONE, service.status(null, true, TODAY));
    }

    @Test
    void yesterdayIsOverdue() {
        assertEquals(DueDateStatus.OVERDUE, service.status(TODAY.minusDays(1), false, TODAY));
    }

    @Test
    void todayIsDueToday() {
        // Due dates are date-only, so this holds at any time of day, including
        // midnight.
        assertEquals(DueDateStatus.DUE_TODAY, service.status(TODAY, false, TODAY));
    }

    @Test
    void tomorrowIsUpcoming() {
        assertEquals(DueDateStatus.UPCOMING, service.status(TODAY.plusDays(1), false, TODAY));
    }

    @Test
    void futureDateIsUpcoming() {
        assertEquals(DueDateStatus.UPCOMING, service.status(TODAY.plusDays(30), false, TODAY));
    }

    @Test
    void finalColumnCardWithPastDueDateIsNotOverdue() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY.minusDays(1), true, TODAY));
    }

    @Test
    void finalColumnCardWithTodayDueDateIsCompleted() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY, true, TODAY));
    }

    @Test
    void finalColumnCardWithFutureDueDateIsCompleted() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY.plusDays(1), true, TODAY));
    }

    @Test
    void sameDueDateYieldsDifferentStatusByFinalColumn() {
        LocalDate yesterday = TODAY.minusDays(1);
        assertEquals(DueDateStatus.OVERDUE, service.status(yesterday, false, TODAY));
        assertEquals(DueDateStatus.COMPLETED, service.status(yesterday, true, TODAY));
    }

    @Test
    void defaultOverloadUsesCurrentDate() {
        assertEquals(DueDateStatus.NONE, service.status(null, false));
        assertEquals(DueDateStatus.OVERDUE, service.status(LocalDate.now().minusDays(1), false));
        assertEquals(DueDateStatus.DUE_TODAY, service.status(LocalDate.now(), false));
        assertEquals(DueDateStatus.UPCOMING, service.status(LocalDate.now().plusDays(1), false));
        assertEquals(DueDateStatus.COMPLETED, service.status(LocalDate.now().minusDays(1), true));
    }

    @Test
    void displayTextUpcomingIncludesFormattedDate() {
        assertEquals("Due Sep 12", service.displayText(DueDateStatus.UPCOMING, LocalDate.of(2026, 9, 12)));
    }

    @Test
    void displayTextDueToday() {
        assertEquals("Due today", service.displayText(DueDateStatus.DUE_TODAY, TODAY));
    }

    @Test
    void displayTextOverdue() {
        assertEquals("Overdue", service.displayText(DueDateStatus.OVERDUE, TODAY.minusDays(1)));
    }

    @Test
    void displayTextCompleted() {
        assertEquals("Completed", service.displayText(DueDateStatus.COMPLETED, TODAY.minusDays(1)));
    }

    @Test
    void displayTextNoneIsEmpty() {
        assertEquals("", service.displayText(DueDateStatus.NONE, null));
    }

    @Test
    void styleClassMapsToCssClasses() {
        assertEquals("due-normal", service.styleClass(DueDateStatus.UPCOMING));
        assertEquals("due-today", service.styleClass(DueDateStatus.DUE_TODAY));
        assertEquals("due-overdue", service.styleClass(DueDateStatus.OVERDUE));
        assertEquals("due-completed", service.styleClass(DueDateStatus.COMPLETED));
        assertNull(service.styleClass(DueDateStatus.NONE));
    }
}
