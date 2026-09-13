package com.boardly.taskboard.repository;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.impl.BoardColumnRepositoryImpl;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.service.WorkspaceService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for card persistence and card business rules against
 * a temporary SQLite database. The real application database is never
 * touched.
 */
class CardIntegrationTest {

    private static Path tempDir;
    private static DatabaseManager db;
    private static WorkspaceRepository workspaceRepository;
    private static BoardRepository boardRepository;
    private static BoardColumnRepository columnRepository;
    private static CardRepository cardRepository;
    private static WorkspaceService workspaceService;
    private static BoardService boardService;
    private static CardService cardService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-card-test");
        db = new DatabaseManager(tempDir.resolve("taskboard.db"));
        db.initialize();
        workspaceRepository = new WorkspaceRepositoryImpl(db);
        boardRepository = new BoardRepositoryImpl(db);
        columnRepository = new BoardColumnRepositoryImpl(db);
        cardRepository = new CardRepositoryImpl(db);
        workspaceService = new WorkspaceService(workspaceRepository);
        boardService = new BoardService(boardRepository, cardRepository, columnRepository, workspaceRepository);
        cardService = new CardService(cardRepository, boardRepository, columnRepository);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void cardInsertPersistsAndAssignsId() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);

        Card created = cardService.createCard(board.getId(), "First task", "Details",
                plannedCol.getId(), LocalDate.of(2026, 10, 1));

        assertNotNull(created.getId());
        Optional<Card> found = cardRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("First task", found.get().getTitle());
        assertEquals("Details", found.get().getDescription());
        assertEquals(plannedCol.getId(), found.get().getBoardColumnId());
        assertEquals(1.0, found.get().getPosition());
        assertEquals(LocalDate.of(2026, 10, 1), found.get().getDueDate());
        assertNull(found.get().getCompletedAt());
    }

    @Test
    void markdownDescriptionPersistsRawText() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);

        String markdown = "# Heading\n**bold** and *italic*\n- [ ] task one\n- [x] task two\n```\ncode line\n```";
        Card card = cardService.createCard(board.getId(), "MD card", markdown,
                plannedCol.getId(), null);

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals(markdown, found.getDescription());
    }

    @Test
    void toggledTaskPersistsRawMarkdown() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);
        MarkdownService markdownService = new MarkdownService();

        String markdown = "# Heading\n- [ ] first\n- [x] second";
        Card card = cardService.createCard(board.getId(), "MD card", markdown,
                plannedCol.getId(), null);

        String toggled = markdownService.toggleTask(card.getDescription(), 0);
        cardService.updateCard(card.getId(), card.getTitle(), toggled,
                card.getBoardColumnId(), card.getDueDate());

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals("# Heading\n- [x] first\n- [x] second", found.getDescription());
    }

    @Test
    void cardWithoutDueDatePersistsNullDueDate() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);

        Card card = cardService.createCard(board.getId(), "No due date", null, plannedCol.getId(), null);

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertNull(found.getDueDate());
    }

    @Test
    void cardUpdatePersistsAllFields() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);
        BoardColumn inProgressCol = columns.get(1);

        Card card = cardService.createCard(board.getId(), "Old title", "Old description",
                plannedCol.getId(), null);

        cardService.updateCard(card.getId(), "New title", "New description",
                inProgressCol.getId(), LocalDate.of(2026, 11, 1));

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals("New title", found.getTitle());
        assertEquals("New description", found.getDescription());
        assertEquals(inProgressCol.getId(), found.getBoardColumnId());
        assertEquals(LocalDate.of(2026, 11, 1), found.getDueDate());
    }

    @Test
    void cardMovePersistsColumnAndOrderTransactionally() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Drag board", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);
        BoardColumn inProgressCol = columns.get(1);

        Card first = cardService.createCard(board.getId(), "First", null, plannedCol.getId(), null);
        Card second = cardService.createCard(board.getId(), "Second", null, plannedCol.getId(), null);
        Card target = cardService.createCard(board.getId(), "Target", null, inProgressCol.getId(), null);

        cardService.moveCard(first.getId(), plannedCol.getId(), 1);
        cardService.moveCard(first.getId(), inProgressCol.getId(), 1);

        List<Card> reloaded = cardRepository.findByBoard(board.getId());
        assertEquals(List.of(second.getId()), reloaded.stream()
                .filter(card -> card.getBoardColumnId() == plannedCol.getId())
                .map(Card::getId).toList());
        assertEquals(List.of(first.getId(), target.getId()), reloaded.stream()
                .filter(card -> card.getBoardColumnId() == inProgressCol.getId())
                .map(Card::getId).toList());
    }

    @Test
    void completedAtIsSetWhenEnteringCompletedAndClearedWhenLeaving() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        BoardColumn plannedCol = columns.get(0);
        BoardColumn inProgressCol = columns.get(1);
        BoardColumn completedCol = columns.get(2);

        Card card = cardService.createCard(board.getId(), "Task", null, plannedCol.getId(), null);

        cardService.updateCard(card.getId(), "Task", null, completedCol.getId(), null);
        Card completed = cardRepository.findById(card.getId()).orElseThrow();
        assertNotNull(completed.getCompletedAt(), "entering COMPLETED must set completed_at");

        cardService.updateCard(card.getId(), "Task", null, inProgressCol.getId(), null);
        Card reopened = cardRepository.findById(card.getId()).orElseThrow();
        assertNull(reopened.getCompletedAt(), "leaving COMPLETED must clear completed_at");
    }

    @Test
    void deleteCardRemovesCard() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());

        Card card = cardService.createCard(board.getId(), "Task", null, columns.get(0).getId(), null);

        assertTrue(cardService.deleteCard(card.getId()));
        assertTrue(cardRepository.findById(card.getId()).isEmpty());
    }

    @Test
    void deleteCardReturnsFalseForUnknownCard() {
        assertFalse(cardService.deleteCard(999));
    }

    @Test
    void cardInsertFailsForUnknownBoard() {
        // Foreign key enforcement: a card cannot reference a missing board.
        assertThrows(DatabaseException.class,
                () -> cardRepository.insert(new Card(999, 1, "Orphan card", null, 1.0, null)));
    }

    @Test
    void updateCardRejectsUnknownCard() {
        assertThrows(ValidationException.class,
                () -> cardService.updateCard(999, "Task", null, 1, null));
    }

    @Test
    void deletingBoardRemovesItsCards() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        List<BoardColumn> columns = columnRepository.findByBoard(board.getId());
        Card card = cardService.createCard(board.getId(), "Task", null, columns.get(0).getId(), null);

        // Deleting the workspace cascades to boards and cards.
        assertTrue(workspaceService.delete(workspace.getId()));
        assertTrue(cardRepository.findById(card.getId()).isEmpty(),
                "the card must be removed with its board");
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
