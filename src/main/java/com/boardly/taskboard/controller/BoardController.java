package com.boardly.taskboard.controller;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.AttachmentService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.DueDateService;
import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.service.NavigationService;
import com.boardly.taskboard.view.CardView;

import javafx.application.HostServices;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Board view: Trello-like kanban columns for the four card statuses.
 *
 * <p>
 * Phase 5: cards can be created per column, edited by clicking a
 * card, and deleted from the card editor. Phase 6: cards show their
 * due date with a semantic CSS class. Phase 7: card descriptions are
 * rendered as Markdown (headings, lists, code, links, task lists) and
 * task-list checkboxes toggle and persist. Card business rules live in
 * {@link CardService}, {@link DueDateService} and
 * {@link MarkdownService}; styling lives in board.css and card.css.
 * </p>
 */
public class BoardController {

    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    private Board board;
    private final Workspace workspace;
    private final NavigationService navigationService;
    private final BoardService boardService;
    private final CardService cardService;
    private final AttachmentService attachmentService;
    private final DueDateService dueDateService;
    private final MarkdownService markdownService;
    private final HostServices hostServices;

    @FXML
    private Label workspaceLabel;

    @FXML
    private Label boardLabel;

    @FXML
    private VBox plannedColumn;

    @FXML
    private VBox inProgressColumn;

    @FXML
    private VBox completedColumn;

    @FXML
    private VBox closedColumn;

    @FXML
    private VBox plannedCards;

    @FXML
    private VBox inProgressCards;

    @FXML
    private VBox completedCards;

    @FXML
    private VBox closedCards;

    @FXML
    private Button plannedNewCardButton;

    @FXML
    private Button inProgressNewCardButton;

    @FXML
    private Button completedNewCardButton;

    @FXML
    private Button closedNewCardButton;

    public BoardController(Board board,
            Workspace workspace,
            NavigationService navigationService,
            BoardService boardService,
            CardService cardService,
            AttachmentService attachmentService,
            DueDateService dueDateService,
            MarkdownService markdownService,
            HostServices hostServices) {
        this.board = board;
        this.workspace = workspace;
        this.navigationService = navigationService;
        this.boardService = boardService;
        this.cardService = cardService;
        this.attachmentService = attachmentService;
        this.dueDateService = dueDateService;
        this.markdownService = markdownService;
        this.hostServices = hostServices;
    }

    @FXML
    private void initialize() {
        workspaceLabel.setText("Workspace: " + workspace.getName());
        boardLabel.setText(board.getName());

        layoutColumns();
        configureDropTarget(plannedCards, plannedColumn, CardStatus.PLANNED);
        configureDropTarget(inProgressCards, inProgressColumn, CardStatus.IN_PROGRESS);
        configureDropTarget(completedCards, completedColumn, CardStatus.COMPLETED);
        configureDropTarget(closedCards, closedColumn, CardStatus.CLOSED);
        refreshBoard();
    }

    @FXML
    private void onBack() {
        navigationService.showWorkspace(workspace);
    }

    @FXML
    private void onNewCard(Event event) {
        CardStatus status = statusOfNewCardButton((Button) event.getSource());
        if (status != null) {
            showCardDialog(null, status);
        }
    }

    private void refreshBoard() {
        try {
            board = boardService.loadBoard(board.getId());
        } catch (AppException e) {
            logger.error("Failed to refresh board {}", board.getId(), e);
        }

        Map<CardStatus, List<Card>> cardsByStatus = groupCardsByStatus();
        plannedCards.getChildren().setAll(cardViews(cardsByStatus.get(CardStatus.PLANNED)));
        inProgressCards.getChildren().setAll(cardViews(cardsByStatus.get(CardStatus.IN_PROGRESS)));
        completedCards.getChildren().setAll(cardViews(cardsByStatus.get(CardStatus.COMPLETED)));
        closedCards.getChildren().setAll(cardViews(cardsByStatus.get(CardStatus.CLOSED)));
    }

    private void openCardEditor(Card card) {
        showCardDialog(card, card.getStatus());
    }

    private void toggleCardTask(Card card, int taskIndex) {
        try {
            String updatedDescription = markdownService.toggleTask(card.getDescription(), taskIndex);
            cardService.updateCard(card.getId(), card.getTitle(), updatedDescription,
                    card.getStatus(), card.getDueDate());
            refreshBoard();
        } catch (AppException e) {
            logger.error("Failed to toggle task on card {}", card.getId(), e);
            showFailure("Unable to update the card. Please try again.");
        }
    }

    private void showCardDialog(Card existing, CardStatus defaultStatus) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.CARD_DIALOG_FXML));
            Stage dialogStage = new Stage();
            CardDialogController controller = new CardDialogController(
                    cardService, board.getId(), existing, defaultStatus, this::refreshBoard, dialogStage,
                    markdownService, hostServices, attachmentService);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.CARD_DIALOG_WIDTH, AppConfig.CARD_DIALOG_HEIGHT);
            scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
            scene.getStylesheets().add(resolveResource(AppConfig.CARD_CSS).toExternalForm());
            dialogStage.setTitle(existing == null ? "New Card" : "Edit Card");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigationService.getStage());
            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open card dialog", e);
            showFailure("Unable to open the card dialog. Please try again.");
        }
    }

    private void showFailure(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Boardly");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private URL resolveResource(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Required resource not found on classpath: " + resourcePath);
        }
        return url;
    }

    private CardStatus statusOfNewCardButton(Button button) {
        if (button == plannedNewCardButton) {
            return CardStatus.PLANNED;
        }
        if (button == inProgressNewCardButton) {
            return CardStatus.IN_PROGRESS;
        }
        if (button == completedNewCardButton) {
            return CardStatus.COMPLETED;
        }
        if (button == closedNewCardButton) {
            return CardStatus.CLOSED;
        }
        return null;
    }

    private void layoutColumns() {
        for (VBox column : List.of(plannedColumn, inProgressColumn, completedColumn, closedColumn)) {
            column.setMinWidth(240);
            column.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(column, Priority.ALWAYS);
            VBox.setVgrow(column, Priority.ALWAYS);
        }
    }

    private Map<CardStatus, List<Card>> groupCardsByStatus() {
        Map<CardStatus, List<Card>> byStatus = new EnumMap<>(CardStatus.class);
        List<Card> cards = board.getCards();
        if (cards != null) {
            for (Card card : cards) {
                byStatus.computeIfAbsent(card.getStatus(), status -> new ArrayList<>()).add(card);
            }
        }
        return byStatus;
    }

    private List<CardView> cardViews(List<Card> cards) {
        List<CardView> views = new ArrayList<>();
        if (cards != null) {
            for (Card card : cards) {
                CardView cardView = new CardView(card, dueDateService, markdownService,
                        this::openCardEditor, this::toggleCardTask, hostServices,
                        attachmentService::resolveById, attachmentService.countForCard(card.getId()));
                configureDragSource(cardView);
                views.add(cardView);
            }
        }
        return views;
    }

    private void configureDragSource(CardView cardView) {
        cardView.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            Dragboard dragboard = cardView.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(Long.toString(cardView.getCard().getId()));
            dragboard.setContent(content);
            WritableImage dragImage = cardView.snapshot(new SnapshotParameters(), null);
            dragboard.setDragView(dragImage, event.getX(), event.getY());
            cardView.setOpacity(0.45);
            cardView.setOnDragDone(doneEvent -> cardView.setOpacity(1.0));
            event.consume();
        });
    }

    private void configureDropTarget(VBox targetCards, VBox targetColumn, CardStatus targetStatus) {
        EventHandler<DragEvent> dragOverHandler = event -> {
            if (isCardDrag(event)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        };
        targetCards.setOnDragOver(dragOverHandler);
        targetColumn.setOnDragOver(dragOverHandler);
        targetCards.setOnDragDropped(event -> handleCardDrop(event, targetCards, targetStatus));
        targetColumn.setOnDragDropped(event -> handleCardDrop(event, targetCards, targetStatus));
        targetColumn.setOnDragEntered(event -> {
            if (isCardDrag(event)) {
                targetColumn.getStyleClass().add("board-column-drag-target");
            }
        });
        targetColumn.setOnDragExited(event -> targetColumn.getStyleClass().remove("board-column-drag-target"));
    }

    private void handleCardDrop(DragEvent event, VBox targetCards, CardStatus targetStatus) {
        boolean completed = false;
        try {
            if (!isCardDrag(event)) {
                return;
            }
            long cardId = Long.parseLong(event.getDragboard().getString());
            cardService.moveCard(cardId, targetStatus, dropIndex(targetCards, event.getY()));
            refreshBoard();
            completed = true;
        } catch (AppException | NumberFormatException e) {
            logger.error("Failed to move card by drag and drop", e);
            showFailure("Unable to move the card. Please try again.");
        } finally {
            targetCards.getParent().getStyleClass().remove("board-column-drag-target");
            event.setDropCompleted(completed);
            event.consume();
        }
    }

    private boolean isCardDrag(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        return dragboard.hasString() && event.getGestureSource() instanceof CardView;
    }

    private int dropIndex(VBox targetCards, double y) {
        for (int index = 0; index < targetCards.getChildren().size(); index++) {
            if (targetCards.getChildren().get(index) instanceof CardView cardView
                    && y < cardView.getLayoutY() + cardView.getBoundsInParent().getHeight() / 2) {
                return index;
            }
        }
        return targetCards.getChildren().size();
    }
}
