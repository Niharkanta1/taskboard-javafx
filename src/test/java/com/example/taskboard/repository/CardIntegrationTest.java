package com.example.taskboard.repository;

import com.example.taskboard.database.DatabaseManager;
import com.example.taskboard.exception.DatabaseException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Board;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.repository.impl.BoardRepositoryImpl;
import com.example.taskboard.repository.impl.CardRepositoryImpl;
import com.example.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.example.taskboard.service.BoardService;
import com.example.taskboard.service.CardService;
import com.example.taskboard.service.MarkdownService;
import com.example.taskboard.service.WorkspaceService;

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
        cardRepository = new CardRepositoryImpl(db);
        workspaceService = new WorkspaceService(workspaceRepository);
        boardService = new BoardService(boardRepository, cardRepository, workspaceRepository);
        cardService = new CardService(cardRepository, boardRepository);
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

        Card created = cardService.createCard(board.getId(), "First task", "Details",
                CardStatus.PLANNED, LocalDate.of(2026, 10, 1));

        assertNotNull(created.getId());
        Optional<Card> found = cardRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("First task", found.get().getTitle());
        assertEquals("Details", found.get().getDescription());
        assertEquals(CardStatus.PLANNED, found.get().getStatus());
        assertEquals(1.0, found.get().getPosition());
        assertEquals(LocalDate.of(2026, 10, 1), found.get().getDueDate());
        assertNull(found.get().getCompletedAt());
    }

    @Test
    void markdownDescriptionPersistsRawText() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);

        String markdown = "# Heading\n**bold** and *italic*\n- [ ] task one\n- [x] task two\n```\ncode line\n```";
        Card card = cardService.createCard(board.getId(), "MD card", markdown,
                CardStatus.PLANNED, null);

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals(markdown, found.getDescription());
    }

    @Test
    void toggledTaskPersistsRawMarkdown() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        MarkdownService markdownService = new MarkdownService();

        String markdown = "# Heading\n- [ ] first\n- [x] second";
        Card card = cardService.createCard(board.getId(), "MD card", markdown,
                CardStatus.PLANNED, null);

        String toggled = markdownService.toggleTask(card.getDescription(), 0);
        cardService.updateCard(card.getId(), card.getTitle(), toggled,
                card.getStatus(), card.getDueDate());

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals("# Heading\n- [x] first\n- [x] second", found.getDescription());
    }

    @Test
    void cardWithoutDueDatePersistsNullDueDate() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);

        Card card = cardService.createCard(board.getId(), "No due date", null, CardStatus.PLANNED, null);

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertNull(found.getDueDate());
    }

    @Test
    void cardUpdatePersistsAllFields() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        Card card = cardService.createCard(board.getId(), "Old title", "Old description",
                CardStatus.PLANNED, null);

        cardService.updateCard(card.getId(), "New title", "New description",
                CardStatus.IN_PROGRESS, LocalDate.of(2026, 11, 1));

        Card found = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals("New title", found.getTitle());
        assertEquals("New description", found.getDescription());
        assertEquals(CardStatus.IN_PROGRESS, found.getStatus());
        assertEquals(LocalDate.of(2026, 11, 1), found.getDueDate());
    }

    @Test
    void cardMovePersistsColumnAndOrderTransactionally() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Drag board", null);
        Card first = cardService.createCard(board.getId(), "First", null, CardStatus.PLANNED, null);
        Card second = cardService.createCard(board.getId(), "Second", null, CardStatus.PLANNED, null);
        Card target = cardService.createCard(board.getId(), "Target", null, CardStatus.IN_PROGRESS, null);

        cardService.moveCard(first.getId(), CardStatus.PLANNED, 1);
        cardService.moveCard(first.getId(), CardStatus.IN_PROGRESS, 1);

        List<Card> reloaded = cardRepository.findByBoard(board.getId());
        assertEquals(List.of(second.getId()), reloaded.stream()
                .filter(card -> card.getStatus() == CardStatus.PLANNED)
                .map(Card::getId).toList());
        assertEquals(List.of(target.getId(), first.getId()), reloaded.stream()
                .filter(card -> card.getStatus() == CardStatus.IN_PROGRESS)
                .map(Card::getId).toList());
        assertEquals(1.0, reloaded.stream().filter(card -> card.getId() == target.getId())
                .findFirst().orElseThrow().getPosition());
        assertEquals(2.0, reloaded.stream().filter(card -> card.getId() == first.getId())
                .findFirst().orElseThrow().getPosition());
    }

    @Test
    void completedAtIsSetWhenEnteringCompletedAndClearedWhenLeaving() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        Card card = cardService.createCard(board.getId(), "Task", null, CardStatus.PLANNED, null);

        cardService.updateCard(card.getId(), "Task", null, CardStatus.COMPLETED, null);
        Card completed = cardRepository.findById(card.getId()).orElseThrow();
        assertNotNull(completed.getCompletedAt(), "entering COMPLETED must set completed_at");

        cardService.updateCard(card.getId(), "Task", null, CardStatus.IN_PROGRESS, null);
        Card reopened = cardRepository.findById(card.getId()).orElseThrow();
        assertNull(reopened.getCompletedAt(), "leaving COMPLETED must clear completed_at");
    }

    @Test
    void deleteCardRemovesCard() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        Card card = cardService.createCard(board.getId(), "Task", null, CardStatus.PLANNED, null);

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
                () -> cardRepository.insert(new Card(999, "Orphan card", null, CardStatus.PLANNED, 1.0, null)));
    }

    @Test
    void updateCardRejectsUnknownCard() {
        assertThrows(ValidationException.class,
                () -> cardService.updateCard(999, "Task", null, CardStatus.PLANNED, null));
    }

    @Test
    void deletingBoardRemovesItsCards() {
        Workspace workspace = workspaceService.create("Development", null);
        Board board = boardService.createBoard(workspace.getId(), "Project", null);
        Card card = cardService.createCard(board.getId(), "Task", null, CardStatus.PLANNED, null);

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
