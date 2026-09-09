package com.example.taskboard.service;

import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Board;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.repository.BoardRepository;
import com.example.taskboard.repository.CardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CardService using in-memory repositories, so no
 * database is required.
 */
class CardServiceTest {

    private FakeBoardRepository boardRepository;
    private FakeCardRepository cardRepository;
    private CardService service;

    @BeforeEach
    void setUp() {
        boardRepository = new FakeBoardRepository();
        cardRepository = new FakeCardRepository();
        service = new CardService(cardRepository, boardRepository);
    }

    @Test
    void createCardSucceedsAndAssignsId() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card created = service.createCard(board.getId(), "  First task  ", "  Details  ",
                CardStatus.PLANNED, LocalDate.of(2026, 10, 1));

        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);
        assertEquals("First task", created.getTitle());
        assertEquals("Details", created.getDescription());
        assertEquals(CardStatus.PLANNED, created.getStatus());
        assertEquals(1.0, created.getPosition());
        assertEquals(LocalDate.of(2026, 10, 1), created.getDueDate());
        assertNull(created.getCompletedAt());
    }

    @Test
    void createCardStoresNullDescriptionWhenBlank() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card created = service.createCard(board.getId(), "Task", "   ", CardStatus.PLANNED, null);
        assertNull(created.getDescription());
    }

    @Test
    void createCardRejectsBlankTitle() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), "   ", null, CardStatus.PLANNED, null));
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), null, null, CardStatus.PLANNED, null));
    }

    @Test
    void createCardRejectsTitleLongerThanMaximum() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        String tooLong = "t".repeat(CardService.TITLE_MAX_LENGTH + 1);
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), tooLong, null, CardStatus.PLANNED, null));
    }

    @Test
    void createCardRejectsDescriptionLongerThanMaximum() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        String tooLong = "d".repeat(CardService.DESCRIPTION_MAX_LENGTH + 1);
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), "Task", tooLong, CardStatus.PLANNED, null));
    }

    @Test
    void createCardRejectsUnknownBoard() {
        assertThrows(ValidationException.class,
                () -> service.createCard(999, "Task", null, CardStatus.PLANNED, null));
    }

    @Test
    void createCardWithCompletedStatusSetsCompletedAt() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card created = service.createCard(board.getId(), "Done", null, CardStatus.COMPLETED, null);
        assertNotNull(created.getCompletedAt());
    }

    @Test
    void createCardAssignsNextPosition() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card first = service.createCard(board.getId(), "First", null, CardStatus.PLANNED, null);
        Card second = service.createCard(board.getId(), "Second", null, CardStatus.IN_PROGRESS, null);

        assertEquals(1.0, first.getPosition());
        assertEquals(2.0, second.getPosition());
    }

    @Test
    void updateCardChangesFields() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Old title", "Old description",
                CardStatus.PLANNED, null);

        Card updated = service.updateCard(card.getId(), "New title", "New description",
                CardStatus.IN_PROGRESS, LocalDate.of(2026, 11, 1));

        assertEquals("New title", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        assertEquals(CardStatus.IN_PROGRESS, updated.getStatus());
        assertEquals(LocalDate.of(2026, 11, 1), updated.getDueDate());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void updateCardEnteringCompletedSetsCompletedAt() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Task", null, CardStatus.PLANNED, null);

        Card updated = service.updateCard(card.getId(), "Task", null, CardStatus.COMPLETED, null);

        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void updateCardLeavingCompletedClearsCompletedAt() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Task", null, CardStatus.COMPLETED, null);
        assertNotNull(card.getCompletedAt());

        Card updated = service.updateCard(card.getId(), "Task", null, CardStatus.IN_PROGRESS, null);

        assertNull(updated.getCompletedAt());
    }

    @Test
    void updateCardLeavingCompletedToClosedClearsCompletedAt() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Task", null, CardStatus.COMPLETED, null);
        assertNotNull(card.getCompletedAt());

        Card updated = service.updateCard(card.getId(), "Task", null, CardStatus.CLOSED, null);

        assertNull(updated.getCompletedAt());
    }

    @Test
    void updateCardStayingCompletedKeepsOriginalCompletedAt() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Task", null, CardStatus.COMPLETED, null);
        Instant original = card.getCompletedAt();

        Card updated = service.updateCard(card.getId(), "Renamed", null, CardStatus.COMPLETED, null);

        assertEquals(original, updated.getCompletedAt());
    }

    @Test
    void updateCardRejectsUnknownCard() {
        assertThrows(ValidationException.class,
                () -> service.updateCard(999, "Task", null, CardStatus.PLANNED, null));
    }

    @Test
    void updateCardRejectsBlankTitle() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Task", null, CardStatus.PLANNED, null);
        assertThrows(ValidationException.class,
                () -> service.updateCard(card.getId(), "   ", null, CardStatus.PLANNED, null));
    }

    @Test
    void deleteCardRemovesCard() {
        Board board = boardRepository.insert(new Board(1, "Project", null));
        Card card = service.createCard(board.getId(), "Task", null, CardStatus.PLANNED, null);

        assertTrue(service.deleteCard(card.getId()));
        assertTrue(cardRepository.findById(card.getId()).isEmpty());
    }

    @Test
    void deleteCardReturnsFalseForUnknownCard() {
        assertFalse(service.deleteCard(999));
    }

    /** Simple in-memory stand-in for BoardRepository in unit tests. */
    private static final class FakeBoardRepository implements BoardRepository {

        private final Map<Long, Board> byId = new HashMap<>();
        private long nextId = 1;

        @Override
        public Board insert(Board board) {
            board.setId(nextId++);
            byId.put(board.getId(), board);
            return board;
        }

        @Override
        public Optional<Board> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Board> findByWorkspace(long workspaceId) {
            List<Board> result = new ArrayList<>();
            for (Board board : byId.values()) {
                if (board.getWorkspaceId() == workspaceId) {
                    result.add(board);
                }
            }
            result.sort(Comparator.comparingLong(Board::getId));
            return result;
        }

        @Override
        public Board update(Board board) {
            if (byId.get(board.getId()) == null) {
                throw new IllegalStateException("Board not found: " + board.getId());
            }
            byId.put(board.getId(), board);
            return board;
        }

        @Override
        public int delete(long id) {
            return byId.remove(id) != null ? 1 : 0;
        }

        @Override
        public long count() {
            return byId.size();
        }
    }

    /** Simple in-memory stand-in for CardRepository in unit tests. */
    private static final class FakeCardRepository implements CardRepository {

        private final Map<Long, Card> byId = new HashMap<>();
        private long nextId = 1;

        @Override
        public Card insert(Card card) {
            card.setId(nextId++);
            byId.put(card.getId(), card);
            return card;
        }

        @Override
        public Optional<Card> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Card> findByBoard(long boardId) {
            List<Card> result = new ArrayList<>();
            for (Card card : byId.values()) {
                if (card.getBoardId() == boardId) {
                    result.add(card);
                }
            }
            result.sort(Comparator.comparingDouble(Card::getPosition).thenComparingLong(Card::getId));
            return result;
        }

        @Override
        public Card update(Card card) {
            byId.put(card.getId(), card);
            return card;
        }

        @Override
        public int delete(long id) {
            return byId.remove(id) != null ? 1 : 0;
        }

        @Override
        public long count() {
            return byId.size();
        }
    }
}
