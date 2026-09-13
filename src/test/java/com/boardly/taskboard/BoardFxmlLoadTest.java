package com.boardly.taskboard;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.controller.BoardController;
import com.boardly.taskboard.controller.BoardDialogController;
import com.boardly.taskboard.controller.CardDialogController;
import com.boardly.taskboard.controller.ColumnDialogController;
import com.boardly.taskboard.controller.DeleteDialogController;
import com.boardly.taskboard.controller.WorkspaceDialogController;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.service.DueDateService;
import com.boardly.taskboard.service.MarkdownService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BoardFxmlLoadTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    @Test
    void boardFxmlLoadsSuccessfully() throws Exception {
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("board-fxml-test");
        com.boardly.taskboard.database.DatabaseManager db = new com.boardly.taskboard.database.DatabaseManager(
                tempDir.resolve("test.db"));
        db.initialize();

        com.boardly.taskboard.repository.WorkspaceRepository workspaceRepo = new com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl(
                db);
        com.boardly.taskboard.repository.BoardRepository boardRepo = new com.boardly.taskboard.repository.impl.BoardRepositoryImpl(
                db);
        com.boardly.taskboard.repository.BoardColumnRepository columnRepo = new com.boardly.taskboard.repository.impl.BoardColumnRepositoryImpl(
                db);
        com.boardly.taskboard.repository.TagRepository tagRepo = new com.boardly.taskboard.repository.impl.TagRepositoryImpl(
                db);
        com.boardly.taskboard.repository.CardRepository cardRepo = new com.boardly.taskboard.repository.impl.CardRepositoryImpl(
                db);
        com.boardly.taskboard.repository.CardAttachmentRepository attachRepo = new com.boardly.taskboard.repository.impl.CardAttachmentRepositoryImpl(
                db);

        com.boardly.taskboard.service.WorkspaceService workspaceService = new com.boardly.taskboard.service.WorkspaceService(
                workspaceRepo);
        com.boardly.taskboard.service.BoardService boardService = new com.boardly.taskboard.service.BoardService(
                boardRepo, cardRepo, columnRepo, workspaceRepo, tagRepo);
        com.boardly.taskboard.service.CardService cardService = new com.boardly.taskboard.service.CardService(cardRepo,
                boardRepo, columnRepo, tagRepo);
        com.boardly.taskboard.service.TagService tagService = new com.boardly.taskboard.service.TagService(tagRepo);
        com.boardly.taskboard.service.ColumnService columnService = new com.boardly.taskboard.service.ColumnService(
                columnRepo, cardRepo, cardService);
        com.boardly.taskboard.service.AttachmentService attachmentService = new com.boardly.taskboard.service.AttachmentService(
                tempDir.resolve("attachments"), attachRepo);

        Workspace workspace = workspaceService.create("Test Workspace", "desc");
        Board board = boardService.createBoard(workspace.getId(), "Sprint 1", "desc");

        // Add some cards with metadata and tags
        List<BoardColumn> cols = columnRepo.findByBoard(board.getId());
        Tag bugTag = tagService.createTag("BugTag", "#ef4444");
        cardService.createCard(board.getId(), "Card 1", "Desc 1", cols.get(0).getId(), java.time.LocalDate.now(),
                com.boardly.taskboard.model.CardPriority.HIGH, com.boardly.taskboard.model.CardSeverity.CRITICAL,
                List.of(bugTag.getId()));

        Board loadedBoard = boardService.loadBoard(board.getId());

        BoardController controller = new BoardController(loadedBoard, workspace, null, boardService, cardService,
                columnService, tagService, attachmentService, new DueDateService(), new MarkdownService(), null);

        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.BOARD_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);

        db.close();
    }

    @Test
    void columnDialogFxmlLoadsSuccessfully() throws Exception {
        ColumnDialogController controller = new ColumnDialogController(null, null, 1L, null, null, null, null);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.COLUMN_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
    }

    @Test
    void tagDialogFxmlLoadsSuccessfully() throws Exception {
        com.boardly.taskboard.controller.TagDialogController controller = new com.boardly.taskboard.controller.TagDialogController(
                null, null, null);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.TAG_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
    }

    @Test
    void cardDialogFxmlLoadsSuccessfully() throws Exception {
        CardDialogController controller = new CardDialogController(
                null, 1L, null, (BoardColumn) null, null, new MarkdownService(), null);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.CARD_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
    }

    @Test
    void boardDialogFxmlLoadsSuccessfully() throws Exception {
        BoardDialogController controller = new BoardDialogController(null, 1L, null, null, null);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.BOARD_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
    }

    @Test
    void workspaceDialogFxmlLoadsSuccessfully() throws Exception {
        WorkspaceDialogController controller = new WorkspaceDialogController(null, null, null, null, 1L);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.WORKSPACE_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
    }

    @Test
    void deleteDialogFxmlLoadsSuccessfully() throws Exception {
        DeleteDialogController controller = new DeleteDialogController("Delete", "Are you sure?", null);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.DELETE_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
    }
}
