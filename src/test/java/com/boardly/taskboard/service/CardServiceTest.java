package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;

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
    private FakeBoardColumnRepository columnRepository;
    private FakeCardRepository cardRepository;
    private CardService service;
    private Board board;
    private BoardColumn plannedCol;
    private BoardColumn inProgressCol;
    private BoardColumn completedCol;
    private BoardColumn closedCol;

    @BeforeEach
    void setUp() {
        boardRepository = new FakeBoardRepository();
        columnRepository = new FakeBoardColumnRepository();
        cardRepository = new FakeCardRepository();
        service = new CardService(cardRepository, boardRepository, columnRepository);

        board = boardRepository.insert(new Board(1, "Project", null));
        plannedCol = columnRepository.insert(new BoardColumn(board.getId(), "Planned", 1.0, false));
        inProgressCol = columnRepository.insert(new BoardColumn(board.getId(), "In Progress", 2.0, false));
        completedCol = columnRepository.insert(new BoardColumn(board.getId(), "Completed", 3.0, true));
        closedCol = columnRepository.insert(new BoardColumn(board.getId(), "Closed", 4.0, true));
    }

    @Test
    void createCardSucceedsAndAssignsId() {
        Card created = service.createCard(board.getId(), "  First task  ", "  Details  ",
                plannedCol.getId(), LocalDate.of(2026, 10, 1));

        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);
        assertEquals("First task", created.getTitle());
        assertEquals("Details", created.getDescription());
        assertEquals(plannedCol.getId(), created.getBoardColumnId());
        assertEquals(1.0, created.getPosition());
        assertEquals(LocalDate.of(2026, 10, 1), created.getDueDate());
        assertNull(created.getCompletedAt());
    }

    @Test
    void createCardStoresNullDescriptionWhenBlank() {
        Card created = service.createCard(board.getId(), "Task", "   ", plannedCol.getId(), null);
        assertNull(created.getDescription());
    }

    @Test
    void createCardRejectsBlankTitle() {
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), "   ", null, plannedCol.getId(), null));
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), null, null, plannedCol.getId(), null));
    }

    @Test
    void createCardRejectsTitleLongerThanMaximum() {
        String tooLong = "t".repeat(CardService.TITLE_MAX_LENGTH + 1);
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), tooLong, null, plannedCol.getId(), null));
    }

    @Test
    void createCardRejectsDescriptionLongerThanMaximum() {
        String tooLong = "d".repeat(CardService.DESCRIPTION_MAX_LENGTH + 1);
        assertThrows(ValidationException.class,
                () -> service.createCard(board.getId(), "Task", tooLong, plannedCol.getId(), null));
    }

    @Test
    void createCardRejectsUnknownBoard() {
        assertThrows(ValidationException.class,
                () -> service.createCard(999, "Task", null, plannedCol.getId(), null));
    }

    @Test
    void createCardWithCompletedStatusSetsCompletedAt() {
        Card created = service.createCard(board.getId(), "Done", null, completedCol.getId(), null);
        assertNotNull(created.getCompletedAt());
    }

    @Test
    void createCardAssignsNextPosition() {
        Card first = service.createCard(board.getId(), "First", null, plannedCol.getId(), null);
        Card second = service.createCard(board.getId(), "Second", null, plannedCol.getId(), null);

        assertEquals(1.0, first.getPosition());
        assertEquals(2.0, second.getPosition());
    }

    @Test
    void updateCardChangesFields() {
        Card card = service.createCard(board.getId(), "Old title", "Old description",
                plannedCol.getId(), null);

        Card updated = service.updateCard(card.getId(), "New title", "New description",
                inProgressCol.getId(), LocalDate.of(2026, 11, 1));

        assertEquals("New title", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        assertEquals(inProgressCol.getId(), updated.getBoardColumnId());
        assertEquals(LocalDate.of(2026, 11, 1), updated.getDueDate());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void updateCardEnteringCompletedSetsCompletedAt() {
        Card card = service.createCard(board.getId(), "Task", null, plannedCol.getId(), null);

        Card updated = service.updateCard(card.getId(), "Task", null, completedCol.getId(), null);

        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void updateCardLeavingCompletedClearsCompletedAt() {
        Card card = service.createCard(board.getId(), "Task", null, completedCol.getId(), null);
        assertNotNull(card.getCompletedAt());

        Card updated = service.updateCard(card.getId(), "Task", null, inProgressCol.getId(), null);

        assertNull(updated.getCompletedAt());
    }

    @Test
    void updateCardLeavingCompletedToClosedKeepsCompletedAt() {
        Card card = service.createCard(board.getId(), "Task", null, completedCol.getId(), null);
        assertNotNull(card.getCompletedAt());
        Instant original = card.getCompletedAt();

        Card updated = service.updateCard(card.getId(), "Task", null, closedCol.getId(), null);

        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void updateCardStayingCompletedKeepsOriginalCompletedAt() {
        Card card = service.createCard(board.getId(), "Task", null, completedCol.getId(), null);
        Instant original = card.getCompletedAt();

        Card updated = service.updateCard(card.getId(), "Renamed", null, completedCol.getId(), null);

        assertEquals(original, updated.getCompletedAt());
    }

    @Test
    void updateCardRejectsUnknownCard() {
        assertThrows(ValidationException.class,
                () -> service.updateCard(999, "Task", null, plannedCol.getId(), null));
    }

    @Test
    void updateCardRejectsBlankTitle() {
        Card card = service.createCard(board.getId(), "Task", null, plannedCol.getId(), null);
        assertThrows(ValidationException.class,
                () -> service.updateCard(card.getId(), "   ", null, plannedCol.getId(), null));
    }

    @Test
    void moveCardReordersWithinColumn() {
        Card first = service.createCard(board.getId(), "First", null, plannedCol.getId(), null);
        Card second = service.createCard(board.getId(), "Second", null, plannedCol.getId(), null);
        Card third = service.createCard(board.getId(), "Third", null, plannedCol.getId(), null);

        service.moveCard(first.getId(), plannedCol.getId(), 3);

        List<Card> ordered = cardRepository.findByBoard(board.getId());
        assertEquals(List.of(second.getId(), third.getId(), first.getId()),
                ordered.stream().map(Card::getId).toList());
    }

    @Test
    void moveCardChangesColumnAndCompletedTimestampRules() {
        Card planned = service.createCard(board.getId(), "Planned", null, plannedCol.getId(), null);
        Card existing = service.createCard(board.getId(), "Existing", null, inProgressCol.getId(), null);

        Card moved = service.moveCard(planned.getId(), inProgressCol.getId(), 2);

        assertEquals(inProgressCol.getId(), moved.getBoardColumnId());
        assertNull(moved.getCompletedAt());
        assertEquals(List.of(existing.getId(), planned.getId()),
                cardRepository.findByBoard(board.getId()).stream()
                        .filter(card -> card.getBoardColumnId() == inProgressCol.getId())
                        .map(Card::getId).toList());

        Card completed = service.moveCard(planned.getId(), completedCol.getId(), 1);
        assertNotNull(completed.getCompletedAt());
        assertEquals(completedCol.getId(), completed.getBoardColumnId());
    }

    @Test
    void deleteCardRemovesCard() {
        Card card = service.createCard(board.getId(), "Task", null, plannedCol.getId(), null);

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

    /** Simple in-memory stand-in for BoardColumnRepository in unit tests. */
    private static final class FakeBoardColumnRepository implements BoardColumnRepository {

        private final Map<Long, BoardColumn> byId = new HashMap<>();
        private long nextId = 1;

        @Override
        public BoardColumn insert(BoardColumn column) {
            column.setId(nextId++);
            byId.put(column.getId(), column);
            return column;
        }

        @Override
        public Optional<BoardColumn> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<BoardColumn> findByBoard(long boardId) {
            List<BoardColumn> result = new ArrayList<>();
            for (BoardColumn col : byId.values()) {
                if (col.getBoardId() == boardId) {
                    result.add(col);
                }
            }
            result.sort(Comparator.comparingDouble(BoardColumn::getPosition).thenComparingLong(BoardColumn::getId));
            return result;
        }

        @Override
        public BoardColumn update(BoardColumn column) {
            byId.put(column.getId(), column);
            return column;
        }

        @Override
        public boolean delete(long id) {
            return byId.remove(id) != null;
        }

        @Override
        public long countByBoard(long boardId) {
            return byId.values().stream().filter(c -> c.getBoardId() == boardId).count();
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
        public void reorder(long boardId, List<CardPlacement> placements) {
            for (CardPlacement placement : placements) {
                Card card = byId.get(placement.cardId());
                if (card != null) {
                    card.setBoardColumnId(placement.boardColumnId());
                    card.setPosition(placement.position());
                    card.setUpdatedAt(placement.updatedAt());
                    card.setCompletedAt(placement.completedAt());
                }
            }
        }

        @Override
        public void reorder(CardPlacement placement) {
            Card card = byId.get(placement.cardId());
            if (card != null) {
                card.setBoardColumnId(placement.boardColumnId());
                card.setPosition(placement.position());
                card.setUpdatedAt(placement.updatedAt());
                card.setCompletedAt(placement.completedAt());
            }
        }

        @Override
        public boolean delete(long id) {
            return byId.remove(id) != null;
        }

        @Override
        public long count() {
            return byId.size();
        }
    }
}
