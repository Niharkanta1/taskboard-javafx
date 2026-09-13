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
        Board board = new Board(1L, 1L, "Sprint 1", "desc", null, null,
                List.of(new BoardColumn(1L, 1L, "Planned", 1.0, false, null, null)),
                List.of());
        Workspace workspace = new Workspace("Workspace 1", "desc");
        BoardController controller = new BoardController(board, workspace, null, null, null, null,
                null, new DueDateService(), new MarkdownService(), null);

        FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.BOARD_FXML));
        loader.setController(controller);
        Parent root = loader.load();
        assertNotNull(root);
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
