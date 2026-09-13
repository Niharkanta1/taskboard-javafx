package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.WorkspaceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for BoardService using in-memory repositories, so no
 * database is required.
 */
class BoardServiceTest {

    private FakeBoardRepository boardRepository;
    private FakeCardRepository cardRepository;
    private FakeBoardColumnRepository columnRepository;
    private FakeWorkspaceRepository workspaceRepository;
    private BoardService service;

    @BeforeEach
    void setUp() {
        boardRepository = new FakeBoardRepository();
        cardRepository = new FakeCardRepository();
        columnRepository = new FakeBoardColumnRepository();
        workspaceRepository = new FakeWorkspaceRepository();
        service = new BoardService(boardRepository, cardRepository, columnRepository, workspaceRepository);
    }

    @Test
    void createBoardSucceedsAndAssignsIdAndSeedsDefaultColumns() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        Board created = service.createBoard(workspace.getId(), "Software Project", "Main project");
        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);
        assertEquals("Software Project", created.getName());
        assertEquals("Main project", created.getDescription());
        assertEquals(workspace.getId(), created.getWorkspaceId());

        List<BoardColumn> columns = columnRepository.findByBoard(created.getId());
        assertEquals(4, columns.size());
        assertEquals(List.of("Planned", "In Progress", "Completed", "Closed"),
                columns.stream().map(BoardColumn::getName).toList());
    }

    @Test
    void createBoardTrimsNameAndDescription() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        Board created = service.createBoard(workspace.getId(), "  Software Project  ", "  Main project  ");
        assertEquals("Software Project", created.getName());
        assertEquals("Main project", created.getDescription());
    }

    @Test
    void createBoardStoresNullDescriptionWhenBlank() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        Board created = service.createBoard(workspace.getId(), "Software Project", "   ");
        assertEquals("Software Project", created.getName());
        assertNull(created.getDescription());
    }

    @Test
    void createBoardRejectsBlankName() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        assertThrows(ValidationException.class, () -> service.createBoard(workspace.getId(), "   ", null));
        assertThrows(ValidationException.class, () -> service.createBoard(workspace.getId(), null, null));
    }

    @Test
    void createBoardRejectsNameLongerThanMaximum() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        String tooLong = "b".repeat(BoardService.NAME_MAX_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.createBoard(workspace.getId(), tooLong, null));
    }

    @Test
    void createBoardRejectsDescriptionLongerThanMaximum() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        String tooLong = "d".repeat(BoardService.DESCRIPTION_MAX_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.createBoard(workspace.getId(), "Board", tooLong));
    }

    @Test
    void createBoardRejectsUnknownWorkspace() {
        assertThrows(ValidationException.class, () -> service.createBoard(999, "Board", null));
    }

    @Test
    void loadBoardReturnsBoardWithCards() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        Board board = service.createBoard(workspace.getId(), "Software Project", null);
        List<BoardColumn> cols = columnRepository.findByBoard(board.getId());

        cardRepository.insert(new Card(board.getId(), cols.get(3).getId(), "Third", null, 3.0, null));
        cardRepository.insert(new Card(board.getId(), cols.get(0).getId(), "First", "details", 1.0, null));
        cardRepository.insert(new Card(board.getId(), cols.get(1).getId(), "Second", null, 2.0, null));

        Board loaded = service.loadBoard(board.getId());
        assertEquals("Software Project", loaded.getName());
        assertEquals(3, loaded.getCards().size());
        assertEquals("First", loaded.getCards().get(0).getTitle());
        assertEquals("Second", loaded.getCards().get(1).getTitle());
        assertEquals("Third", loaded.getCards().get(2).getTitle());
    }

    @Test
    void loadBoardFailsForUnknownId() {
        assertThrows(ValidationException.class, () -> service.loadBoard(999));
    }

    @Test
    void findBoardsByWorkspaceReturnsOnlyThatWorkspacesBoards() {
        Workspace development = workspaceRepository.insert(new Workspace("Development", null));
        Workspace personal = workspaceRepository.insert(new Workspace("Personal", null));

        service.createBoard(development.getId(), "Dev Board", null);
        service.createBoard(personal.getId(), "Personal Board", null);
        service.createBoard(development.getId(), "Dev Board 2", null);

        List<Board> developmentBoards = service.findBoardsByWorkspace(development.getId());
        assertEquals(2, developmentBoards.size());
        assertTrue(developmentBoards.stream().allMatch(b -> b.getWorkspaceId() == development.getId()));
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

    /** Minimal in-memory stand-in for WorkspaceRepository in unit tests. */
    private static final class FakeWorkspaceRepository implements WorkspaceRepository {

        private final Map<Long, Workspace> byId = new HashMap<>();
        private long nextId = 1;

        @Override
        public Workspace insert(Workspace workspace) {
            workspace.setId(nextId++);
            byId.put(workspace.getId(), workspace);
            return workspace;
        }

        @Override
        public Optional<Workspace> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Workspace> findAll() {
            List<Workspace> list = new ArrayList<>(byId.values());
            list.sort(Comparator.comparingLong(Workspace::getId));
            return list;
        }

        @Override
        public List<Workspace> findAllByOwner(long ownerUserId) {
            List<Workspace> list = new ArrayList<>();
            for (Workspace w : byId.values()) {
                if (w.getOwnerUserId() == ownerUserId) {
                    list.add(w);
                }
            }
            list.sort(Comparator.comparingLong(Workspace::getId));
            return list;
        }

        @Override
        public Workspace update(Workspace workspace) {
            byId.put(workspace.getId(), workspace);
            return workspace;
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
