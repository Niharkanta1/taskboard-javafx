package com.boardly.taskboard.database;

import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.WorkspaceRepository;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests covering database initialization, migrations,
 * foreign-key enforcement, transactions and cascade deletes.
 *
 * <p>Uses a temporary database file so the real application database is
 * never touched.</p>
 */
class DatabaseIntegrationTest {

    private static Path tempDir;
    private static Path dbPath;
    private static DatabaseManager db;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-test");
        dbPath = tempDir.resolve("taskboard.db");
        db = new DatabaseManager(dbPath);
        db.initialize();
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void dataDirectoryAndDatabaseFileAreCreated() {
        assertTrue(Files.isDirectory(dbPath.getParent()), "data directory should exist");
        assertTrue(Files.isRegularFile(dbPath), "database file should exist");
    }

    @Test
    void allTablesExist() throws Exception {
        // db.getConnection() returns the shared application connection; it must NOT be closed here.
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            Set<String> tables = new HashSet<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            for (String expected : new String[]{"users", "workspaces", "boards", "cards", "card_attachments", "flyway_schema_history"}) {
                assertTrue(tables.contains(expected), "Missing table: " + expected);
            }
        }
    }

    @Test
    void migrationIsRecordedExactlyOnce() throws Exception {
        // db.getConnection() returns the shared application connection; it must NOT be closed here.
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void reinitializationDoesNotReapplyMigrations() throws Exception {
        DatabaseManager second = new DatabaseManager(dbPath);
        try {
            second.initialize();
            try (Connection conn = second.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1'");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "migration must not be applied twice");
            }
        } finally {
            second.close();
        }
    }

    @Test
    void foreignKeysAreEnforced() {
        assertThrows(DatabaseException.class, () -> db.inTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO boards (workspace_id, name, created_at, updated_at) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, 99999);
                ps.setString(2, "Orphan board");
                ps.setString(3, Instant.now().toString());
                ps.setString(4, Instant.now().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Expected foreign key violation", e);
            }
            return null;
        }), "inserting a board with a non-existent workspace must fail");
    }

    @Test
    void failedTransactionRollsBackAllStatements() throws Exception {
        WorkspaceRepository workspaces = new WorkspaceRepositoryImpl(db);
        long before = workspaces.count();
        assertThrows(DatabaseException.class, () -> db.inTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workspaces (name, description, created_at, updated_at) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, "Temp workspace");
                ps.setString(2, null);
                ps.setString(3, Instant.now().toString());
                ps.setString(4, Instant.now().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("First statement failed unexpectedly", e);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO boards (workspace_id, name, created_at, updated_at) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, 99999);
                ps.setString(2, "Bad board");
                ps.setString(3, Instant.now().toString());
                ps.setString(4, Instant.now().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Expected foreign key violation", e);
            }
            return null;
        }), "second statement must fail");
        assertEquals(before, workspaces.count(), "first statement must have been rolled back");
    }

    @Test
    void deletingWorkspaceCascadesToBoardsAndCards() throws Exception {
        WorkspaceRepository workspaces = new WorkspaceRepositoryImpl(db);
        BoardRepository boards = new BoardRepositoryImpl(db);
        CardRepository cards = new CardRepositoryImpl(db);

        Workspace workspace = workspaces.insert(new Workspace("Cascade Test", null));
        Board board = boards.insert(new Board(workspace.getId(), "Board A", null));
        Card card = cards.insert(new Card(board.getId(), "Card A", null, CardStatus.PLANNED, 1.0, null));

        // db.getConnection() returns the shared application connection; it must NOT be closed here.
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM workspaces WHERE id = ?")) {
            ps.setLong(1, workspace.getId());
            assertEquals(1, ps.executeUpdate());
        }

        assertFalse(workspaces.findById(workspace.getId()).isPresent(), "workspace should be deleted");
        assertFalse(boards.findById(board.getId()).isPresent(), "board should be cascade-deleted");
        assertFalse(cards.findById(card.getId()).isPresent(), "card should be cascade-deleted");
    }

    @Test
    void repositoryInsertAndReadBackWorks() {
        WorkspaceRepository workspaces = new WorkspaceRepositoryImpl(db);
        Workspace saved = workspaces.insert(new Workspace("Read Back", "some description"));
        assertTrue(saved.getId() > 0, "insert should assign an id");

        Optional<Workspace> found = workspaces.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Read Back", found.get().getName());
        assertEquals("some description", found.get().getDescription());
        assertTrue(workspaces.count() >= 1);
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
