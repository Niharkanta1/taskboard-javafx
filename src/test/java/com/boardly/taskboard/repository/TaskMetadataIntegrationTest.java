package com.boardly.taskboard.repository;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardPriority;
import com.boardly.taskboard.model.CardSeverity;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.impl.BoardColumnRepositoryImpl;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.TagRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.TagService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskMetadataIntegrationTest {

    private static Path tempDir;
    private static Path dbPath;
    private static DatabaseManager db;
    private static WorkspaceRepository workspaceRepository;
    private static BoardRepository boardRepository;
    private static BoardColumnRepository columnRepository;
    private static CardRepository cardRepository;
    private static TagRepository tagRepository;
    private static WorkspaceService workspaceService;
    private static BoardService boardService;
    private static CardService cardService;
    private static TagService tagService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-metadata-test");
        dbPath = tempDir.resolve("taskboard.db");
        db = new DatabaseManager(dbPath);
        db.initialize();
        workspaceRepository = new WorkspaceRepositoryImpl(db);
        boardRepository = new BoardRepositoryImpl(db);
        columnRepository = new BoardColumnRepositoryImpl(db);
        tagRepository = new TagRepositoryImpl(db);
        cardRepository = new CardRepositoryImpl(db);
        workspaceService = new WorkspaceService(workspaceRepository);
        boardService = new BoardService(boardRepository, cardRepository, columnRepository, workspaceRepository,
                tagRepository);
        cardService = new CardService(cardRepository, boardRepository, columnRepository, tagRepository);
        tagService = new TagService(tagRepository);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void taskPrioritySeverityAndTagsPersistAcrossRestart() {
        Workspace workspace = workspaceService.create("Metadata Team", null);
        Board board = boardService.createBoard(workspace.getId(), "Metadata Board", "testing");
        List<BoardColumn> cols = columnRepository.findByBoard(board.getId());

        Tag urgentTag = tagService.createTag("Security", "#ef4444");
        Tag backendTag = tagService.createTag("Backend", "#0ea5e9");

        Card card = cardService.createCard(board.getId(), "Fix Security Issue", "urgent fix",
                cols.get(0).getId(), LocalDate.of(2026, 9, 30),
                CardPriority.URGENT, CardSeverity.CRITICAL,
                List.of(urgentTag.getId(), backendTag.getId()));

        assertNotNull(card.getId());
        assertEquals(CardPriority.URGENT, card.getPriority());
        assertEquals(CardSeverity.CRITICAL, card.getSeverity());
        assertEquals(2, card.getTags().size());

        // Simulate app restart
        DatabaseManager restartDb = new DatabaseManager(dbPath);
        restartDb.initialize();

        CardRepository restartCardRepo = new CardRepositoryImpl(restartDb);
        TagRepository restartTagRepo = new TagRepositoryImpl(restartDb);
        BoardRepository restartBoardRepo = new BoardRepositoryImpl(restartDb);
        BoardColumnRepository restartColRepo = new BoardColumnRepositoryImpl(restartDb);
        BoardService restartBoardService = new BoardService(restartBoardRepo, restartCardRepo, restartColRepo, null,
                restartTagRepo);

        Board reloadedBoard = restartBoardService.loadBoard(board.getId());
        Card reloadedCard = reloadedBoard.getCards().stream().filter(c -> c.getId() == card.getId()).findFirst()
                .orElseThrow();

        assertEquals(CardPriority.URGENT, reloadedCard.getPriority());
        assertEquals(CardSeverity.CRITICAL, reloadedCard.getSeverity());
        assertEquals(2, reloadedCard.getTags().size());
        assertTrue(reloadedCard.getTags().stream().anyMatch(t -> t.getName().equals("Security")));
        assertTrue(reloadedCard.getTags().stream().anyMatch(t -> t.getName().equals("Backend")));

        restartDb.close();
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
