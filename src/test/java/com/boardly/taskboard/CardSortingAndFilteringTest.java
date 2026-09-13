package com.boardly.taskboard;

import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardPriority;
import com.boardly.taskboard.model.CardSeverity;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.model.TaskSortOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardSortingAndFilteringTest {

    private Card card1;
    private Card card2;
    private Card card3;
    private Card card4;
    private List<Card> cards;

    @BeforeEach
    void setUp() {
        Tag bug = new Tag(1L, "Bug", "#ef4444", Instant.now(), Instant.now());
        Tag feature = new Tag(2L, "Feature", "#0ea5e9", Instant.now(), Instant.now());

        card1 = new Card(1L, 1L, 1L, "Alpha bug", "Fix auth", 1.0, LocalDate.of(2026, 9, 20),
                Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-09-01T10:00:00Z"), null,
                CardPriority.LOW, CardSeverity.BLOCKER, List.of(bug));

        card2 = new Card(2L, 1L, 1L, "Beta feature", "Add exports", 2.0, LocalDate.of(2026, 9, 15),
                Instant.parse("2026-09-05T10:00:00Z"), Instant.parse("2026-09-05T10:00:00Z"), null,
                CardPriority.URGENT, CardSeverity.MINOR, List.of(feature));

        card3 = new Card(3L, 1L, 1L, "Gamma refactor", "Clean code", 3.0, null,
                Instant.parse("2026-09-10T10:00:00Z"), Instant.parse("2026-09-10T10:00:00Z"), null,
                CardPriority.HIGH, CardSeverity.CRITICAL, List.of(bug, feature));

        card4 = new Card(4L, 1L, 1L, "Delta docs", "Write readme", 4.0, LocalDate.of(2026, 9, 10),
                Instant.parse("2026-09-12T10:00:00Z"), Instant.parse("2026-09-12T10:00:00Z"), null,
                CardPriority.MEDIUM, CardSeverity.LOW, List.of());

        cards = new ArrayList<>(List.of(card1, card2, card3, card4));
    }

    @Test
    void defaultSortIsPriorityDescending() {
        cards.sort(TaskSortOption.PRIORITY_DESC.getComparator());
        // Priorities: card2 (URGENT=4), card3 (HIGH=3), card4 (MEDIUM=2), card1 (LOW=1)
        assertEquals(List.of(card2, card3, card4, card1), cards);
    }

    @Test
    void sortPriorityAscending() {
        cards.sort(TaskSortOption.PRIORITY_ASC.getComparator());
        assertEquals(List.of(card1, card4, card3, card2), cards);
    }

    @Test
    void sortSeverityDescending() {
        cards.sort(TaskSortOption.SEVERITY_DESC.getComparator());
        // Severities: card1 (BLOCKER=5), card3 (CRITICAL=4), card2 (MINOR=2), card4
        // (LOW=1)
        assertEquals(List.of(card1, card3, card2, card4), cards);
    }

    @Test
    void sortSeverityAscending() {
        cards.sort(TaskSortOption.SEVERITY_ASC.getComparator());
        assertEquals(List.of(card4, card2, card3, card1), cards);
    }

    @Test
    void sortDueDateAscending() {
        cards.sort(TaskSortOption.DUE_DATE_ASC.getComparator());
        // card4 (Sep 10), card2 (Sep 15), card1 (Sep 20), card3 (null -> last)
        assertEquals(List.of(card4, card2, card1, card3), cards);
    }

    @Test
    void sortDueDateDescending() {
        cards.sort(TaskSortOption.DUE_DATE_DESC.getComparator());
        // card1 (Sep 20), card2 (Sep 15), card4 (Sep 10), card3 (null -> last)
        assertEquals(List.of(card1, card2, card4, card3), cards);
    }

    @Test
    void sortCreatedOnDescending() {
        cards.sort(TaskSortOption.CREATED_AT_DESC.getComparator());
        // card4 (Sep 12), card3 (Sep 10), card2 (Sep 5), card1 (Sep 1)
        assertEquals(List.of(card4, card3, card2, card1), cards);
    }

    @Test
    void sortCreatedOnAscending() {
        cards.sort(TaskSortOption.CREATED_AT_ASC.getComparator());
        assertEquals(List.of(card1, card2, card3, card4), cards);
    }

    @Test
    void sortNameAscending() {
        cards.sort(TaskSortOption.NAME_ASC.getComparator());
        assertEquals(List.of(card1, card2, card4, card3), cards);
    }

    @Test
    void sortNameDescending() {
        cards.sort(TaskSortOption.NAME_DESC.getComparator());
        assertEquals(List.of(card3, card4, card2, card1), cards);
    }

    @Test
    void multiFilteringWorksTogether() {
        // Filter by text "bug" AND priority LOW AND tag "Bug"
        List<Card> filtered = cards.stream()
                .filter(c -> c.getTitle().toLowerCase().contains("bug"))
                .filter(c -> c.getPriority() == CardPriority.LOW)
                .filter(c -> c.getTags().stream().anyMatch(t -> t.getName().equals("Bug")))
                .toList();

        assertEquals(1, filtered.size());
        assertEquals(card1, filtered.get(0));
    }
}
