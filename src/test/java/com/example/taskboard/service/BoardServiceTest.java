package com.example.taskboard.service;

import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Board;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.repository.BoardRepository;
import com.example.taskboard.repository.CardRepository;
import com.example.taskboard.repository.WorkspaceRepository;

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
    private FakeWorkspaceRepository workspaceRepository;
    private BoardService service;

    @BeforeEach
    void setUp() {
        boardRepository = new FakeBoardRepository();
        cardRepository = new FakeCardRepository();
        workspaceRepository = new FakeWorkspaceRepository();
        service = new BoardService(boardRepository, cardRepository, workspaceRepository);
    }

    @Test
    void createBoardSucceedsAndAssignsId() {
        Workspace workspace = workspaceRepository.insert(new Workspace("Development", null));
        Board created = service.createBoard(workspace.getId(), "Software Project", "Main project");
        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);
        assertEquals("Software Project", created.getName());
        assertEquals("Main project", created.getDescription());
        assertEquals(workspace.getId(), created.getWorkspaceId());
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

        cardRepository.insert(new Card(board.getId(), "Third", null, CardStatus.CLOSED, 3.0, null));
        cardRepository.insert(new Card(board.getId(), "First", "details", CardStatus.PLANNED, 1.0, null));
        cardRepository.insert(new Card(board.getId(), "Second", null, CardStatus.IN_PROGRESS, 2.0, null));

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
        public Workspace update(Workspace workspace) {
            byId.put(workspace.getId(), workspace);
            return workspace;
        }

        @Override
        public Optional<Workspace> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Workspace> findAll() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public long count() {
            return byId.size();
        }

        @Override
        public int delete(long id) {
            return byId.remove(id) != null ? 1 : 0;
        }
    }
}
