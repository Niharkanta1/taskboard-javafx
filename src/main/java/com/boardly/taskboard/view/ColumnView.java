package com.boardly.taskboard.view;

import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.service.DueDateService;
import com.boardly.taskboard.service.MarkdownService;

import javafx.application.HostServices;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A kanban column node: header (name, card count, rename/delete buttons),
 * a card list, and an "Add Card" button.
 *
 * <p>
 * The whole column is a drag source and drag target: cards can be dragged
 * between columns, and columns themselves can be reordered by dragging
 * the column header.
 * </p>
 */
public class ColumnView extends VBox {

    private final BoardColumn column;
    private final Label nameLabel;
    private final Label countLabel;
    private final HBox header;
    private final VBox cardsContainer;
    private final Button addCardButton;

    public ColumnView(BoardColumn column, List<Card> cards,
            MarkdownService markdownService, DueDateService dueDateService,
            Runnable onAddCard, Runnable onRenameColumn, Runnable onDeleteColumn,
            Consumer<Card> onCardClick) {
        this(column, cards, markdownService, dueDateService, onAddCard, onRenameColumn, onDeleteColumn,
                onCardClick, null, null, null, null);
    }

    public ColumnView(BoardColumn column, List<Card> cards,
            MarkdownService markdownService, DueDateService dueDateService,
            Runnable onAddCard, Runnable onRenameColumn, Runnable onDeleteColumn,
            Consumer<Card> onCardClick, BiConsumer<Card, Integer> onTaskToggle,
            HostServices hostServices,
            Function<Long, Optional<Path>> attachmentResolver,
            Function<Long, Integer> attachmentCounter) {
        this.column = column;
        getStyleClass().add("board-column");
        setSpacing(10);

        nameLabel = new Label(column.getName());
        nameLabel.getStyleClass().add("board-column-header");

        countLabel = new Label(String.valueOf(cards != null ? cards.size() : 0));
        countLabel.getStyleClass().add("board-column-count");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button renameButton = new Button("Rename");
        renameButton.getStyleClass().add("column-action-button");
        renameButton.setOnAction(e -> onRenameColumn.run());

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("column-delete-button");
        deleteButton.setOnAction(e -> onDeleteColumn.run());

        header = new HBox(6);
        header.getStyleClass().add("board-column-header-row");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getChildren().addAll(nameLabel, countLabel, headerSpacer, renameButton, deleteButton);

        // Initiate column drag from header
        header.setOnDragDetected(event -> {
            Dragboard db = header.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("column:" + column.getId());
            db.setContent(content);
            event.consume();
        });

        cardsContainer = new VBox(8);
        VBox.setVgrow(cardsContainer, Priority.ALWAYS);

        addCardButton = new Button("+ Add Card");
        addCardButton.getStyleClass().add("column-add-card-button");
        addCardButton.setMaxWidth(Double.MAX_VALUE);
        addCardButton.setOnAction(e -> onAddCard.run());

        getChildren().addAll(header, cardsContainer, addCardButton);
        if (cards != null) {
            rebuildCards(cards, markdownService, dueDateService, onCardClick, onTaskToggle,
                    hostServices, attachmentResolver, attachmentCounter);
        }
    }

    public BoardColumn getColumn() {
        return column;
    }

    /**
     * The column header node. It is the drag source used to reorder columns.
     */
    public HBox getHeaderNode() {
        return header;
    }

    public void setCardCount(int count) {
        countLabel.setText(String.valueOf(count));
    }

    /**
     * Replaces the card list, rebuilding all card views.
     */
    public void rebuildCards(List<Card> cards, MarkdownService markdownService,
            DueDateService dueDateService, Consumer<Card> onCardClick,
            BiConsumer<Card, Integer> onTaskToggle,
            HostServices hostServices,
            Function<Long, Optional<Path>> attachmentResolver,
            Function<Long, Integer> attachmentCounter) {
        cardsContainer.getChildren().clear();
        setCardCount(cards.size());
        for (Card card : cards) {
            int attCount = (attachmentCounter != null) ? attachmentCounter.apply(card.getId()) : 0;
            CardView cardView = new CardView(card, dueDateService, markdownService,
                    onCardClick, onTaskToggle, hostServices, attachmentResolver, attCount);
            cardsContainer.getChildren().add(cardView);
        }
    }

    /**
     * Highlights the column as a valid drop target while a card or column is
     * dragged over it.
     */
    public void setDropTarget(boolean active) {
        if (active) {
            if (!getStyleClass().contains("board-column-drag-target")) {
                getStyleClass().add("board-column-drag-target");
            }
        } else {
            getStyleClass().remove("board-column-drag-target");
        }
    }
}
