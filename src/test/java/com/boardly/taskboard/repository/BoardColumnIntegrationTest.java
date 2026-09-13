package com.boardly.taskboard.repository;

import com.boardly.taskboard.database.DatabaseManager;
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
import com.boardly.taskboard.service.ColumnService;
import com.boardly.taskboard.service.WorkspaceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardColumnIntegrationTest {

    private static Path tempDir;
    private static Path dbPath;
    private static DatabaseManager db;
    private static WorkspaceRepository workspaceRepository;
    private static BoardRepository boardRepository;
    private static BoardColumnRepository columnRepository;
    private static CardRepository cardRepository;
    private static WorkspaceService workspaceService;
    private static BoardService boardService;
    private static CardService cardService;
    private static ColumnService columnService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-column-test");
        dbPath = tempDir.resolve("taskboard.db");
        db = new DatabaseManager(dbPath);
        db.initialize();
        workspaceRepository = new WorkspaceRepositoryImpl(db);
        boardRepository = new BoardRepositoryImpl(db);
        columnRepository = new BoardColumnRepositoryImpl(db);
        cardRepository = new CardRepositoryImpl(db);
        workspaceService = new WorkspaceService(workspaceRepository);
        boardService = new BoardService(boardRepository, cardRepository, columnRepository, workspaceRepository);
        cardService = new CardService(cardRepository, boardRepository, columnRepository);
        columnService = new ColumnService(columnRepository, cardRepository, cardService);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void defaultColumnsAreSeededAndPersistAcrossRestart() throws Exception {
        Workspace workspace = workspaceService.create("Dev Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Sprint Board", "Current sprint");

        List<BoardColumn> initialColumns = columnRepository.findByBoard(board.getId());
        assertEquals(4, initialColumns.size());
        assertEquals(List.of("Planned", "In Progress", "Completed", "Closed"),
                initialColumns.stream().map(BoardColumn::getName).toList());

        // Add custom column and rename one
        BoardColumn qa = columnService.createColumn(board.getId(), "QA", false);
        columnService.renameColumn(initialColumns.get(0).getId(), "Backlog");

        // Simulate app restart by opening a new DatabaseManager connection
        DatabaseManager restartDb = new DatabaseManager(dbPath);
        restartDb.initialize();
        BoardColumnRepository restartColumnRepo = new BoardColumnRepositoryImpl(restartDb);

        List<BoardColumn> reloaded = restartColumnRepo.findByBoard(board.getId());
        assertEquals(5, reloaded.size());
        assertEquals("Backlog", reloaded.get(0).getName());
        assertEquals("QA", reloaded.get(4).getName());

        restartDb.close();
    }

    @Test
    void columnReorderingAndCardMovePersists() {
        Workspace workspace = workspaceService.create("DND Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Reorder Board", null);
        List<BoardColumn> cols = columnRepository.findByBoard(board.getId());

        Card card1 = cardService.createCard(board.getId(), "Task A", "desc", cols.get(0).getId(), null);
        Card card2 = cardService.createCard(board.getId(), "Task B", "desc", cols.get(0).getId(), null);

        // Move card to in progress
        cardService.moveCard(card1.getId(), cols.get(1).getId(), 1);

        // Reorder columns: move column 4 ("Closed") to position 1
        columnService.moveColumn(cols.get(3).getId(), 1);

        List<BoardColumn> reloadedCols = columnRepository.findByBoard(board.getId());
        assertEquals("Closed", reloadedCols.get(0).getName());
        assertEquals("Planned", reloadedCols.get(1).getName());

        List<Card> reloadedCards = cardRepository.findByBoard(board.getId());
        Card reloadedCard1 = reloadedCards.stream().filter(c -> c.getId() == card1.getId()).findFirst().orElseThrow();
        assertEquals(cols.get(1).getId(), reloadedCard1.getBoardColumnId());
    }

    private static void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
