package com.example.taskboard.service;

import com.example.taskboard.model.CardStatus;
import com.example.taskboard.model.DueDateStatus;

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
        for (CardStatus status : CardStatus.values()) {
            assertEquals(DueDateStatus.NONE, service.status(null, status, TODAY));
        }
    }

    @Test
    void yesterdayIsOverdue() {
        assertEquals(DueDateStatus.OVERDUE, service.status(TODAY.minusDays(1), CardStatus.PLANNED, TODAY));
        assertEquals(DueDateStatus.OVERDUE, service.status(TODAY.minusDays(1), CardStatus.IN_PROGRESS, TODAY));
    }

    @Test
    void todayIsDueToday() {
        // Due dates are date-only, so this holds at any time of day, including midnight.
        assertEquals(DueDateStatus.DUE_TODAY, service.status(TODAY, CardStatus.PLANNED, TODAY));
        assertEquals(DueDateStatus.DUE_TODAY, service.status(TODAY, CardStatus.IN_PROGRESS, TODAY));
    }

    @Test
    void tomorrowIsUpcoming() {
        assertEquals(DueDateStatus.UPCOMING, service.status(TODAY.plusDays(1), CardStatus.PLANNED, TODAY));
        assertEquals(DueDateStatus.UPCOMING, service.status(TODAY.plusDays(1), CardStatus.IN_PROGRESS, TODAY));
    }

    @Test
    void futureDateIsUpcoming() {
        assertEquals(DueDateStatus.UPCOMING, service.status(TODAY.plusDays(30), CardStatus.PLANNED, TODAY));
    }

    @Test
    void completedCardWithPastDueDateIsNotOverdue() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY.minusDays(1), CardStatus.COMPLETED, TODAY));
    }

    @Test
    void completedCardWithTodayDueDateIsCompleted() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY, CardStatus.COMPLETED, TODAY));
    }

    @Test
    void completedCardWithFutureDueDateIsCompleted() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY.plusDays(1), CardStatus.COMPLETED, TODAY));
    }

    @Test
    void closedCardWithPastDueDateIsNotOverdue() {
        assertEquals(DueDateStatus.COMPLETED, service.status(TODAY.minusDays(1), CardStatus.CLOSED, TODAY));
    }

    @Test
    void sameDueDateYieldsDifferentStatusByCardStatus() {
        LocalDate yesterday = TODAY.minusDays(1);
        assertEquals(DueDateStatus.OVERDUE, service.status(yesterday, CardStatus.PLANNED, TODAY));
        assertEquals(DueDateStatus.OVERDUE, service.status(yesterday, CardStatus.IN_PROGRESS, TODAY));
        assertEquals(DueDateStatus.COMPLETED, service.status(yesterday, CardStatus.COMPLETED, TODAY));
        assertEquals(DueDateStatus.COMPLETED, service.status(yesterday, CardStatus.CLOSED, TODAY));
    }

    @Test
    void defaultOverloadUsesCurrentDate() {
        assertEquals(DueDateStatus.NONE, service.status(null, CardStatus.PLANNED));
        assertEquals(DueDateStatus.OVERDUE, service.status(LocalDate.now().minusDays(1), CardStatus.PLANNED));
        assertEquals(DueDateStatus.DUE_TODAY, service.status(LocalDate.now(), CardStatus.PLANNED));
        assertEquals(DueDateStatus.UPCOMING, service.status(LocalDate.now().plusDays(1), CardStatus.PLANNED));
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
