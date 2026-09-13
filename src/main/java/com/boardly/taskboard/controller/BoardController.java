package com.boardly.taskboard.controller;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.service.AttachmentService;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.ColumnService;
import com.boardly.taskboard.service.DueDateService;
import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.service.NavigationService;
import com.boardly.taskboard.view.ColumnView;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Kanban board view with dynamic, user-managed columns.
 *
 * <p>
 * Columns are rendered from the persisted board column configuration;
 * users can add, rename, delete, and drag-and-drop reorder columns.
 * Cards can be dragged between columns and reordered within a column.
 * </p>
 */
public class BoardController {

    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    private static final String CARD_DRAG_PREFIX = "card:";
    private static final String COLUMN_DRAG_PREFIX = "column:";

    private Board board;
    private final Workspace workspace;
    private final NavigationService navigationService;
    private final BoardService boardService;
    private final CardService cardService;
    private final ColumnService columnService;
    private final AttachmentService attachmentService;
    private final DueDateService dueDateService;
    private final MarkdownService markdownService;
    private final HostServices hostServices;

    @FXML
    private Label workspaceLabel;

    @FXML
    private Label boardTitleLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button addColumnButton;

    @FXML
    private HBox boardColumns;

    @FXML
    private Button statusOfNewCardButton;

    @FXML
    private Label footerLabel;

    public BoardController(Board board, Workspace workspace, NavigationService navigationService,
            BoardService boardService, CardService cardService,
            ColumnService columnService,
            AttachmentService attachmentService,
            DueDateService dueDateService,
            MarkdownService markdownService,
            HostServices hostServices) {
        this.board = board;
        this.workspace = workspace;
        this.navigationService = navigationService;
        this.boardService = boardService;
        this.cardService = cardService;
        this.columnService = columnService;
        this.attachmentService = attachmentService;
        this.dueDateService = dueDateService;
        this.markdownService = markdownService;
        this.hostServices = hostServices;
    }

    @FXML
    private void initialize() {
        if (workspace != null && workspaceLabel != null) {
            workspaceLabel.setText(workspace.getName());
        }
        if (board != null && boardTitleLabel != null) {
            boardTitleLabel.setText(board.getName());
        }
        if (backButton != null) {
            backButton.setOnAction(e -> onBack());
        }
        if (addColumnButton != null) {
            addColumnButton.setOnAction(e -> onAddColumn());
        }
        if (footerLabel != null) {
            footerLabel.setText("Tip: drag cards between columns, and drag a column header to reorder it.");
        }
        renderColumns();
    }

    @FXML
    private void onBack() {
        if (navigationService != null && workspace != null) {
            navigationService.showWorkspace(workspace);
        }
    }

    /**
     * FXML handler for the footer "Add Card" button: opens the card dialog
     * for the first column of the board.
     */
    @FXML
    private void onAddCard() {
        List<BoardColumn> columns = board.getColumns();
        if (columns.isEmpty()) {
            return;
        }
        onAddCard(columns.get(0));
    }

    /**
     * Rebuilds the column views from the persisted board state.
     */
    private void renderColumns() {
        if (boardService != null && board != null) {
            try {
                board = boardService.loadBoard(board.getId());
            } catch (AppException e) {
                logger.error("Failed to load board {}", board.getId(), e);
                return;
            }
        }
        if (board == null || boardColumns == null) {
            return;
        }
        boardColumns.getChildren().clear();
        List<BoardColumn> columns = board.getColumns() != null ? board.getColumns() : List.of();
        List<Card> cards = board.getCards() != null ? board.getCards() : List.of();
        for (BoardColumn column : columns) {
            List<Card> columnCards = cards.stream()
                    .filter(card -> card.getBoardColumnId() == column.getId())
                    .collect(Collectors.toList());

            ColumnView columnView = new ColumnView(column, columnCards, markdownService,
                    dueDateService,
                    () -> onAddCard(column),
                    () -> onRenameColumn(column),
                    () -> onDeleteColumn(column),
                    card -> onCardClick(card),
                    (card, taskIndex) -> toggleCardTask(card, taskIndex),
                    hostServices,
                    attachmentService != null ? attachmentService::resolveById : null,
                    attachmentService != null ? attachmentService::countForCard : null);

            // Drag over: accept column reorder or card move
            columnView.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    String content = db.getString();
                    if (content.startsWith(COLUMN_DRAG_PREFIX)) {
                        long draggedId = Long.parseLong(content.substring(COLUMN_DRAG_PREFIX.length()));
                        if (draggedId != column.getId()) {
                            event.acceptTransferModes(TransferMode.MOVE);
                            columnView.setDropTarget(true);
                        }
                    } else if (content.startsWith(CARD_DRAG_PREFIX)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        columnView.setDropTarget(true);
                    }
                }
                event.consume();
            });

            columnView.setOnDragExited(event -> {
                columnView.setDropTarget(false);
                event.consume();
            });

            columnView.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                columnView.setDropTarget(false);
                if (db.hasString()) {
                    String content = db.getString();
                    if (content.startsWith(COLUMN_DRAG_PREFIX)) {
                        long draggedId = Long.parseLong(content.substring(COLUMN_DRAG_PREFIX.length()));
                        if (draggedId != column.getId()) {
                            try {
                                int targetIndex = columnViewIndex(columnView);
                                columnService.moveColumn(draggedId, targetIndex);
                                renderColumns();
                                success = true;
                            } catch (AppException e) {
                                showErrorMessage(e);
                            }
                        }
                    } else if (content.startsWith(CARD_DRAG_PREFIX)) {
                        long cardId = Long.parseLong(content.substring(CARD_DRAG_PREFIX.length()));
                        try {
                            int index = cardIndexInColumn(column, cardId);
                            cardService.moveCard(cardId, column.getId(),
                                    index < 0 ? columnCards.size() + 1 : index + 1);
                            renderColumns();
                            success = true;
                        } catch (AppException e) {
                            showErrorMessage(e);
                        }
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            boardColumns.getChildren().add(columnView);
        }
    }

    private void toggleCardTask(Card card, int taskIndex) {
        try {
            String updatedDescription = markdownService.toggleTask(card.getDescription(), taskIndex);
            cardService.updateCard(card.getId(), card.getTitle(), updatedDescription,
                    card.getBoardColumnId(), card.getDueDate());
            renderColumns();
        } catch (AppException e) {
            logger.error("Failed to toggle task in card {}", card.getId(), e);
            showErrorMessage(e);
        }
    }

    /**
     * 1-based index of the column in the board, used when dropping a dragged
     * column.
     */
    private int columnViewIndex(ColumnView columnView) {
        List<javafx.scene.Node> children = boardColumns.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == columnView) {
                return i + 1;
            }
        }
        return children.size();
    }

    /**
     * 0-based index of the card within its column, or -1 if the card is
     * not in this column.
     */
    private int cardIndexInColumn(BoardColumn column, long cardId) {
        List<Card> columnCards = board.getCards().stream()
                .filter(card -> card.getBoardColumnId() == column.getId())
                .collect(Collectors.toList());
        for (int i = 0; i < columnCards.size(); i++) {
            if (columnCards.get(i).getId() == cardId) {
                return i;
            }
        }
        return -1;
    }

    private void onAddCard(BoardColumn column) {
        showCardDialog("New Card",
                dialogStage -> new CardDialogController(cardService, board.getId(), null, column, this::renderColumns,
                        dialogStage, markdownService, hostServices, attachmentService));
    }

    @FXML
    private void onAddColumn() {
        showColumnDialog("New Column",
                dialogStage -> new ColumnDialogController(columnService, dialogStage, board.getId(), null, null, null,
                        this::renderColumns));
    }

    private void onRenameColumn(BoardColumn column) {
        showColumnDialog("Rename Column",
                dialogStage -> new ColumnDialogController(columnService, dialogStage, board.getId(), column, null, null,
                        this::renderColumns));
    }

    private void onDeleteColumn(BoardColumn column) {
        if (board.getColumns().size() <= BoardService.MIN_COLUMNS_PER_BOARD) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(navigationService.getStage());
            alert.setTitle("Cannot Delete Column");
            alert.setHeaderText("Minimum column limit reached");
            alert.setContentText("A board must have at least " + BoardService.MIN_COLUMNS_PER_BOARD + " columns.");
            alert.showAndWait();
            return;
        }
        List<BoardColumn> targetColumns = board.getColumns().stream()
                .filter(c -> !c.getId().equals(column.getId()))
                .collect(Collectors.toList());
        showColumnDialog("Delete Column",
                dialogStage -> new ColumnDialogController(columnService, dialogStage, board.getId(), null, column,
                        targetColumns,
                        this::renderColumns));
    }

    private void onCardClick(Card card) {
        showCardDialog("Edit Card",
                dialogStage -> new CardDialogController(cardService, board.getId(), card, card.getColumn(),
                        this::renderColumns,
                        dialogStage, markdownService, hostServices, attachmentService));
    }

    private boolean showColumnDialog(String title,
            java.util.function.Function<Stage, ColumnDialogController> controllerFactory) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.COLUMN_DIALOG_FXML));
            Stage dialogStage = new Stage();
            ColumnDialogController controller = controllerFactory.apply(dialogStage);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.COLUMN_DIALOG_WIDTH, AppConfig.COLUMN_DIALOG_HEIGHT);
            scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.BOARD_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.CARD_CSS).toExternalForm());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigationService.getStage());
            dialogStage.setTitle(title);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            return true;
        } catch (Exception e) {
            logger.error("Failed to open column dialog", e);
            return false;
        }
    }

    private boolean showCardDialog(String title,
            java.util.function.Function<Stage, CardDialogController> controllerFactory) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.CARD_DIALOG_FXML));
            Stage dialogStage = new Stage();
            CardDialogController controller = controllerFactory.apply(dialogStage);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.CARD_DIALOG_WIDTH, AppConfig.CARD_DIALOG_HEIGHT);
            scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.BOARD_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.CARD_CSS).toExternalForm());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigationService.getStage());
            dialogStage.setTitle(title);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            return true;
        } catch (Exception e) {
            logger.error("Failed to open card dialog", e);
            return false;
        }
    }

    private java.net.URL resolveResource(String resourcePath) {
        java.net.URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Required resource not found on classpath: " + resourcePath);
        }
        return url;
    }

    private void showErrorMessage(AppException e) {
        footerLabel.setText(e.getMessage());
    }
}
