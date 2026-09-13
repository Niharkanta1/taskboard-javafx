package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnServiceTest {

    private FakeBoardColumnRepository columnRepository;
    private FakeCardRepository cardRepository;
    private CardService cardService;
    private ColumnService columnService;
    private long boardId = 1L;

    @BeforeEach
    void setUp() {
        columnRepository = new FakeBoardColumnRepository();
        cardRepository = new FakeCardRepository();
        cardService = new CardService(cardRepository, columnRepository);
        columnService = new ColumnService(columnRepository, cardRepository, cardService);

        // Seed 4 default columns
        columnRepository.insert(new BoardColumn(boardId, "Planned", 1.0, false));
        columnRepository.insert(new BoardColumn(boardId, "In Progress", 2.0, false));
        columnRepository.insert(new BoardColumn(boardId, "Completed", 3.0, true));
        columnRepository.insert(new BoardColumn(boardId, "Closed", 4.0, true));
    }

    @Test
    void createCustomColumnSucceeds() {
        BoardColumn created = columnService.createColumn(boardId, "QA Testing", false);

        assertNotNull(created.getId());
        assertEquals("QA Testing", created.getName());
        assertEquals(5.0, created.getPosition());
        assertFalse(created.isFinal());
        assertEquals(5, columnRepository.findByBoard(boardId).size());
    }

    @Test
    void createColumnRejectsEmptyOrBlankName() {
        assertThrows(ValidationException.class, () -> columnService.createColumn(boardId, "", false));
        assertThrows(ValidationException.class, () -> columnService.createColumn(boardId, "   ", false));
        assertThrows(ValidationException.class, () -> columnService.createColumn(boardId, null, false));
    }

    @Test
    void renameColumnSucceeds() {
        List<BoardColumn> cols = columnRepository.findByBoard(boardId);
        BoardColumn planned = cols.get(0);

        BoardColumn renamed = columnService.renameColumn(planned.getId(), "To Do");

        assertEquals("To Do", renamed.getName());
        assertEquals(planned.getId(), renamed.getId());
    }

    @Test
    void renameColumnRejectsEmptyOrBlankName() {
        List<BoardColumn> cols = columnRepository.findByBoard(boardId);
        BoardColumn planned = cols.get(0);

        assertThrows(ValidationException.class, () -> columnService.renameColumn(planned.getId(), ""));
        assertThrows(ValidationException.class, () -> columnService.renameColumn(planned.getId(), "   "));
        assertThrows(ValidationException.class, () -> columnService.renameColumn(planned.getId(), null));
    }

    @Test
    void reorderColumnMovesToTargetIndex() {
        List<BoardColumn> cols = columnRepository.findByBoard(boardId);
        BoardColumn closed = cols.get(3); // was position 4

        // Move Closed to position 1
        columnService.moveColumn(closed.getId(), 1);

        List<BoardColumn> reordered = columnRepository.findByBoard(boardId);
        assertEquals("Closed", reordered.get(0).getName());
        assertEquals(1.0, reordered.get(0).getPosition());
        assertEquals("Planned", reordered.get(1).getName());
        assertEquals(2.0, reordered.get(1).getPosition());
        assertEquals("In Progress", reordered.get(2).getName());
        assertEquals(3.0, reordered.get(2).getPosition());
        assertEquals("Completed", reordered.get(3).getName());
        assertEquals(4.0, reordered.get(3).getPosition());
    }

    @Test
    void deleteColumnEnforcesMinimumThreeColumnsRule() {
        List<BoardColumn> cols = columnRepository.findByBoard(boardId);
        assertEquals(4, cols.size());

        // Deleting 4th column should leave 3 columns (allowed)
        columnService.deleteColumn(cols.get(3).getId(), cols.get(0).getId());
        assertEquals(3, columnRepository.findByBoard(boardId).size());

        // Deleting when 3 columns left must fail
        List<BoardColumn> remaining = columnRepository.findByBoard(boardId);
        assertThrows(ValidationException.class,
                () -> columnService.deleteColumn(remaining.get(0).getId(), remaining.get(1).getId()));
    }

    @Test
    void deleteColumnMovesExistingCardsToTargetColumn() {
        List<BoardColumn> cols = columnRepository.findByBoard(boardId);
        BoardColumn source = cols.get(3); // Closed
        BoardColumn target = cols.get(2); // Completed

        Card card1 = cardService.createCard(boardId, "Task 1", "Desc", source.getId(), null);
        Card card2 = cardService.createCard(boardId, "Task 2", "Desc", source.getId(), null);

        int moved = columnService.deleteColumn(source.getId(), target.getId());

        assertEquals(2, moved);
        assertFalse(columnRepository.findById(source.getId()).isPresent());
        List<Card> targetCards = cardRepository.findByBoard(boardId).stream()
                .filter(c -> c.getBoardColumnId() == target.getId())
                .toList();
        assertEquals(2, targetCards.size());
        assertEquals(List.of(card1.getId(), card2.getId()), targetCards.stream().map(Card::getId).toList());
    }

    @Test
    void deleteColumnWithCardsRequiresTargetColumn() {
        List<BoardColumn> cols = columnRepository.findByBoard(boardId);
        BoardColumn extra = columnService.createColumn(boardId, "Extra", false);
        cardService.createCard(boardId, "Task", null, extra.getId(), null);

        assertThrows(ValidationException.class, () -> columnService.deleteColumn(extra.getId(), null));
    }

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
        public boolean delete(long id) {
            return byId.remove(id) != null;
        }

        @Override
        public long count() {
            return byId.size();
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
    }
}
