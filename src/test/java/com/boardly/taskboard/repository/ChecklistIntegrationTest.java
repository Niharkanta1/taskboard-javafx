package com.boardly.taskboard.repository;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.Checklist;
import com.boardly.taskboard.model.ChecklistItem;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.impl.BoardColumnRepositoryImpl;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.ChecklistRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.CardService;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for checklist persistence on cards against a
 * temporary SQLite database. The real application database is never
 * touched.
 */
class ChecklistIntegrationTest {

    private static Path tempDir;
    private static Path dbPath;
    private static DatabaseManager db;
    private static WorkspaceRepository workspaceRepository;
    private static BoardRepository boardRepository;
    private static BoardColumnRepository columnRepository;
    private static CardRepository cardRepository;
    private static ChecklistRepository checklistRepository;
    private static WorkspaceService workspaceService;
    private static BoardService boardService;
    private static CardService cardService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-checklist-test");
        dbPath = tempDir.resolve("taskboard.db");
        db = new DatabaseManager(dbPath);
        db.initialize();
        workspaceRepository = new WorkspaceRepositoryImpl(db);
        boardRepository = new BoardRepositoryImpl(db);
        columnRepository = new BoardColumnRepositoryImpl(db);
        checklistRepository = new ChecklistRepositoryImpl(db);
        cardRepository = new CardRepositoryImpl(db, checklistRepository);
        workspaceService = new WorkspaceService(workspaceRepository);
        boardService = new BoardService(boardRepository, cardRepository, columnRepository, workspaceRepository);
        cardService = new CardService(cardRepository, boardRepository, columnRepository, null, checklistRepository);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void checklistsPersistWithNewCard() {
        Workspace workspace = workspaceService.create("Checklist Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Checklist Board", null);
        BoardColumn col = columnRepository.findByBoard(board.getId()).get(0);

        Checklist checklist = checklist("Setup", item("Install dependencies", true), item("Run tests", false));
        Card card = cardService.createCard(board.getId(), "Task with checklist", null,
                col.getId(), null, null, null, null, List.of(checklist));

        assertNotNull(card.getId());
        assertEquals(1, card.getChecklists().size());
        assertEquals("Setup", card.getChecklists().get(0).getTitle());
        assertEquals(2, card.getChecklists().get(0).getItems().size());
        assertNotNull(checklist.getId(), "checklist id must be assigned on insert");

        Card reloaded = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals(1, reloaded.getChecklists().size());
        Checklist reloadedList = reloaded.getChecklists().get(0);
        assertEquals("Setup", reloadedList.getTitle());
        assertEquals(2, reloadedList.getItems().size());
        assertEquals("Install dependencies", reloadedList.getItems().get(0).getText());
        assertTrue(reloadedList.getItems().get(0).isCompleted());
        assertFalse(reloadedList.getItems().get(1).isCompleted());
    }

    @Test
    void checklistsSurviveRestartAndKeepOrder() {
        Workspace workspace = workspaceService.create("Restart Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Restart Board", null);
        BoardColumn col = columnRepository.findByBoard(board.getId()).get(0);

        Card card = cardService.createCard(board.getId(), "Ordered card", null, col.getId(), null,
                null, null, null,
                List.of(checklist("Second", item("b1", false)), checklist("First", item("a1", true))));

        // Simulate an application restart with a fresh connection.
        DatabaseManager restartDb = new DatabaseManager(dbPath);
        restartDb.initialize();
        CardRepository restartCardRepository = new CardRepositoryImpl(
                restartDb, new ChecklistRepositoryImpl(restartDb));

        Card reloaded = restartCardRepository.findById(card.getId()).orElseThrow();
        assertEquals(2, reloaded.getChecklists().size());
        assertEquals("Second", reloaded.getChecklists().get(0).getTitle());
        assertEquals("First", reloaded.getChecklists().get(1).getTitle());
        assertEquals("a1", reloaded.getChecklists().get(1).getItems().get(0).getText());
        assertTrue(reloaded.getChecklists().get(1).getItems().get(0).isCompleted());

        restartDb.close();
    }

    @Test
    void updatingChecklistsReplacesPreviousState() {
        Workspace workspace = workspaceService.create("Update Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Update Board", null);
        BoardColumn col = columnRepository.findByBoard(board.getId()).get(0);

        Card card = cardService.createCard(board.getId(), "Mutable card", null, col.getId(), null,
                null, null, null, List.of(checklist("Old", item("old item", false))));

        // Rename the checklist, toggle the item, add a new item and a new checklist.
        Checklist renamed = checklist("New", item("old item", true), item("new item", false));
        Checklist added = checklist("Extra", item("extra item", false));
        cardService.updateCard(card.getId(), "Mutable card", null, col.getId(), null,
                null, null, null, List.of(renamed, added));

        Card reloaded = cardRepository.findById(card.getId()).orElseThrow();
        assertEquals(2, reloaded.getChecklists().size());
        assertEquals("New", reloaded.getChecklists().get(0).getTitle());
        assertTrue(reloaded.getChecklists().get(0).getItems().get(0).isCompleted());
        assertEquals("new item", reloaded.getChecklists().get(0).getItems().get(1).getText());
        assertEquals("Extra", reloaded.getChecklists().get(1).getTitle());
    }

    @Test
    void removingAllChecklistsClearsThem() {
        Workspace workspace = workspaceService.create("Clear Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Clear Board", null);
        BoardColumn col = columnRepository.findByBoard(board.getId()).get(0);

        Card card = cardService.createCard(board.getId(), "Card to clear", null, col.getId(), null,
                null, null, null, List.of(checklist("Gone", item("item", false))));

        cardService.updateCard(card.getId(), "Card to clear", null, col.getId(), null,
                null, null, null, List.of());

        Card reloaded = cardRepository.findById(card.getId()).orElseThrow();
        assertTrue(reloaded.getChecklists().isEmpty());
    }

    @Test
    void deletingCardCascadesToChecklists() {
        Workspace workspace = workspaceService.create("Delete Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Delete Board", null);
        BoardColumn col = columnRepository.findByBoard(board.getId()).get(0);

        Card card = cardService.createCard(board.getId(), "Doomed card", null, col.getId(), null,
                null, null, null, List.of(checklist("Doomed", item("item", false))));

        assertTrue(cardService.deleteCard(card.getId()));
        assertTrue(checklistRepository.findByCardId(card.getId()).isEmpty(),
                "checklists must be removed with their card");
    }

    @Test
    void blankChecklistTitleIsRejected() {
        Workspace workspace = workspaceService.create("Validate Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Validate Board", null);
        BoardColumn col = columnRepository.findByBoard(board.getId()).get(0);

        assertThrows(ValidationException.class,
                () -> cardService.createCard(board.getId(), "Bad checklist", null, col.getId(), null,
                        null, null, null, List.of(checklist("   ", item("item", false)))));
    }

    private static Checklist checklist(String title, ChecklistItem... items) {
        Checklist checklist = new Checklist();
        checklist.setTitle(title);
        checklist.getItems().addAll(List.of(items));
        return checklist;
    }

    private static ChecklistItem item(String text, boolean completed) {
        ChecklistItem item = new ChecklistItem();
        item.setText(text);
        item.setCompleted(completed);
        return item;
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
