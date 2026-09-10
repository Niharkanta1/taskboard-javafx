package com.boardly.taskboard.repository;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.WorkspaceService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for board and card persistence against a temporary
 * SQLite database. The real application database is never touched.
 */
class BoardIntegrationTest {

    private static Path tempDir;
    private static DatabaseManager db;
    private static WorkspaceRepository workspaceRepository;
    private static BoardRepository boardRepository;
    private static CardRepository cardRepository;
    private static WorkspaceService workspaceService;
    private static BoardService boardService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-board-test");
        db = new DatabaseManager(tempDir.resolve("taskboard.db"));
        db.initialize();
        workspaceRepository = new WorkspaceRepositoryImpl(db);
        boardRepository = new BoardRepositoryImpl(db);
        cardRepository = new CardRepositoryImpl(db);
        workspaceService = new WorkspaceService(workspaceRepository);
        boardService = new BoardService(boardRepository, cardRepository, workspaceRepository);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void boardInsertPersistsAndAssignsId() {
        Workspace workspace = workspaceService.create("Development", null);
        Board created = boardService.createBoard(workspace.getId(), "Software Project", "Main project");

        assertNotNull(created.getId());
        Optional<Board> found = boardRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Software Project", found.get().getName());
        assertEquals("Main project", found.get().getDescription());
        assertEquals(workspace.getId(), found.get().getWorkspaceId());
    }

    @Test
    void findByWorkspaceReturnsOnlyMatchingBoards() {
        Workspace development = workspaceService.create("Development", null);
        Workspace personal = workspaceService.create("Personal", null);

        boardService.createBoard(development.getId(), "Dev Board", null);
        boardService.createBoard(personal.getId(), "Personal Board", null);
        boardService.createBoard(development.getId(), "Dev Board 2", null);

        List<Board> developmentBoards = boardService.findBoardsByWorkspace(development.getId());
        assertEquals(2, developmentBoards.size());
        assertTrue(developmentBoards.stream()
                .allMatch(board -> board.getWorkspaceId() == development.getId()));
    }

    @Test
    void loadBoardReturnsCardsOrderedByPosition() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Software Project", null);

        // Insert out of order to prove the repository orders by position.
        cardRepository.insert(new Card(board.getId(), "Third", null, CardStatus.CLOSED, 3.0, null));
        cardRepository.insert(new Card(board.getId(), "First", "details", CardStatus.PLANNED, 1.0, null));
        cardRepository.insert(new Card(board.getId(), "Second", null, CardStatus.IN_PROGRESS, 2.0, null));

        Board loaded = boardService.loadBoard(board.getId());
        assertEquals(3, loaded.getCards().size());
        assertEquals("First", loaded.getCards().get(0).getTitle());
        assertEquals("Second", loaded.getCards().get(1).getTitle());
        assertEquals("Third", loaded.getCards().get(2).getTitle());
        assertEquals(CardStatus.PLANNED, loaded.getCards().get(0).getStatus());
        assertEquals(CardStatus.IN_PROGRESS, loaded.getCards().get(1).getStatus());
        assertEquals(CardStatus.CLOSED, loaded.getCards().get(2).getStatus());
    }

    @Test
    void loadBoardFailsForUnknownId() {
        assertThrows(ValidationException.class, () -> boardService.loadBoard(999));
    }

    @Test
    void boardInsertFailsForUnknownWorkspace() {
        // Foreign key enforcement: a board cannot reference a missing workspace.
        assertThrows(DatabaseException.class,
                () -> boardRepository.insert(new Board(999, "Orphan Board", null)));
    }

    @Test
    void deletingWorkspaceRemovesItsBoardsAndCards() {
        Workspace workspace = workspaceService.create("Cascade", null);
        Board board = boardService.createBoard(workspace.getId(), "Board 1", null);
        cardRepository.insert(new Card(board.getId(), "Card 1", null, CardStatus.PLANNED, 1.0, null));

        assertTrue(workspaceService.delete(workspace.getId()));
        assertTrue(boardRepository.findById(board.getId()).isEmpty(), "the board must be removed with the workspace");
        assertTrue(cardRepository.findByBoard(board.getId()).isEmpty(), "the board's cards must be removed with the workspace");
    }

    private static void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // best effort cleanup of temp test directory
                }
            });
        }
    }
}
