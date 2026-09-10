package com.boardly.taskboard.repository;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for workspace persistence against a temporary
 * SQLite database. The real application database is never touched.
 */
class WorkspaceIntegrationTest {

    private static Path tempDir;
    private static DatabaseManager db;
    private static WorkspaceRepository workspaceRepository;
    private static BoardRepository boardRepository;
    private static CardRepository cardRepository;
    private static WorkspaceService workspaceService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-workspace-test");
        db = new DatabaseManager(tempDir.resolve("taskboard.db"));
        db.initialize();
        workspaceRepository = new WorkspaceRepositoryImpl(db);
        boardRepository = new BoardRepositoryImpl(db);
        cardRepository = new CardRepositoryImpl(db);
        workspaceService = new WorkspaceService(workspaceRepository);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void createPersistsWorkspace() {
        Workspace created = workspaceService.create("Development", "Work projects");
        assertNotNull(created.getId());

        Optional<Workspace> found = workspaceRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Development", found.get().getName());
        assertEquals("Work projects", found.get().getDescription());
    }

    @Test
    void blankDescriptionIsStoredAsNull() {
        Workspace created = workspaceService.create("NoDescription", "   ");
        Optional<Workspace> found = workspaceRepository.findById(created.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getDescription());
    }

    @Test
    void findAllReturnsCreatedWorkspaces() {
        long before = workspaceRepository.count();
        workspaceService.create("Listed", null);
        assertEquals(before + 1, workspaceRepository.count());
        List<Workspace> all = workspaceRepository.findAll();
        assertTrue(all.stream().anyMatch(w -> w.getName().equals("Listed")));
    }

    @Test
    void updatePersistsChanges() {
        Workspace created = workspaceService.create("Before", "old description");

        Workspace updated = workspaceService.update(created.getId(), "After", "new description");
        assertTrue(!updated.getUpdatedAt().isBefore(created.getUpdatedAt()));

        Workspace found = workspaceRepository.findById(created.getId()).orElseThrow();
        assertEquals("After", found.getName());
        assertEquals("new description", found.getDescription());
    }

    @Test
    void updateRejectsBlankName() {
        Workspace created = workspaceService.create("Kept", null);
        assertThrows(ValidationException.class, () -> workspaceService.update(created.getId(), "   ", null));
    }

    @Test
    void deleteRemovesWorkspace() {
        Workspace created = workspaceService.create("Doomed", null);
        assertTrue(workspaceService.delete(created.getId()));
        assertTrue(workspaceRepository.findById(created.getId()).isEmpty());
    }

    @Test
    void deletingWorkspaceCascadesToBoardsAndCards() {
        Workspace workspace = workspaceService.create("Cascade", null);
        Board board = boardRepository.insert(new Board(workspace.getId(), "Board 1", null));
        Card card = cardRepository.insert(
                new Card(board.getId(), "Card 1", null, CardStatus.PLANNED, 1.0, null));

        assertTrue(workspaceService.delete(workspace.getId()));

        assertEquals(0, boardRepository.count(), "boards must be removed with the workspace");
        assertEquals(0, cardRepository.count(), "cards must be removed with the workspace");
    }

    @Test
    void deleteReturnsFalseForUnknownId() {
        assertFalse(workspaceService.delete(999));
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
