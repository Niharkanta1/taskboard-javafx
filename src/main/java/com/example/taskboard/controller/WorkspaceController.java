package com.example.taskboard.controller;

import com.example.taskboard.config.AppConfig;
import com.example.taskboard.exception.AppException;
import com.example.taskboard.model.Board;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.service.BoardService;
import com.example.taskboard.service.NavigationService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

/**
 * Workspace view: lists the boards of one workspace and opens them.
 *
 * <p>Board business rules live in {@link BoardService}; this controller
 * only handles the view and user interaction. Styling is in CSS.</p>
 */
public class WorkspaceController {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceController.class);

    private final Workspace workspace;
    private final BoardService boardService;
    private final NavigationService navigationService;

    @FXML
    private Label workspaceTitle;

    @FXML
    private ListView<Board> boardList;

    @FXML
    private Label emptyHint;

    public WorkspaceController(Workspace workspace,
                              BoardService boardService,
                              NavigationService navigationService) {
        this.workspace = workspace;
        this.boardService = boardService;
        this.navigationService = navigationService;
    }

    @FXML
    private void initialize() {
        workspaceTitle.setText("Workspace: " + workspace.getName());
        boardList.setCellFactory(lv -> boardCellFactory());
        refreshBoards();
    }

    @FXML
    private void onNewBoard() {
        showBoardDialog(null);
    }

    @FXML
    private void onOpenBoard() {
        Board selected = boardList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            navigationService.showBoard(selected.getId());
        }
    }

    @FXML
    private void onEditBoard() {
        Board selected = boardList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showBoardDialog(selected);
        }
    }

    @FXML
    private void onDeleteBoard() {
        Board selected = boardList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean confirmed = DeleteDialogs.confirmDelete(
                (Stage) boardList.getScene().getWindow(),
                "Delete Board",
                "Delete board \"" + selected.getName() + "\"?\n"
                        + "This will also delete all cards in this board.");
        if (!confirmed) {
            return;
        }

        try {
            boardService.delete(selected.getId());
            refreshBoards();
        } catch (AppException e) {
            logger.error("Failed to delete board", e);
            showFailure("Unable to delete the board. Please try again.");
        }
    }

    @FXML
    private void onBack() {
        navigationService.showDashboard();
    }

    private void refreshBoards() {
        List<Board> boards;
        try {
            boards = boardService.findBoardsByWorkspace(workspace.getId());
        } catch (AppException e) {
            logger.error("Failed to load boards for workspace {}", workspace.getId(), e);
            boards = List.of();
        }
        boardList.getItems().setAll(boards);
        emptyHint.setVisible(boardList.getItems().isEmpty());
    }

    private void showBoardDialog(Board existing) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.BOARD_DIALOG_FXML));
            Stage dialogStage = new Stage();
            BoardDialogController controller = new BoardDialogController(
                    boardService, workspace.getId(), existing, this::refreshBoards, dialogStage);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.BOARD_DIALOG_WIDTH, AppConfig.BOARD_DIALOG_HEIGHT);
            scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.DASHBOARD_CSS).toExternalForm());
            dialogStage.setTitle(existing == null ? "Create Board" : "Edit Board");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigationService.getStage());
            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open board dialog", e);
            showFailure("Unable to open the board dialog. Please try again.");
        }
    }

    private void showFailure(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("TaskBoard");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private ListCell<Board> boardCellFactory() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Board board, boolean empty) {
                super.updateItem(board, empty);
                if (empty || board == null) {
                    setGraphic(null);
                    setText("");
                } else {
                    Label nameLabel = new Label(board.getName());
                    nameLabel.getStyleClass().add("workspace-item-name");
                    VBox box = new VBox(2, nameLabel);
                    String description = board.getDescription();
                    if (description != null && !description.isBlank()) {
                        Label descriptionLabel = new Label(description);
                        descriptionLabel.getStyleClass().add("workspace-item-description");
                        descriptionLabel.setWrapText(true);
                        box.getChildren().add(descriptionLabel);
                    }
                    setGraphic(box);
                    setText("");
                    setOnMouseClicked(event -> {
                        if (!isEmpty() && board != null && event.getClickCount() == 2) {
                            navigationService.showBoard(board.getId());
                        }
                    });
                }
            }
        };
    }

    private URL resolveResource(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Required resource not found on classpath: " + resourcePath);
        }
        return url;
    }
}
