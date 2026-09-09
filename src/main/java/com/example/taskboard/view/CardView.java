package com.example.taskboard.view;

import com.example.taskboard.model.Card;
import com.example.taskboard.model.DueDateStatus;
import com.example.taskboard.service.DueDateService;
import com.example.taskboard.service.MarkdownService;

import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reusable card component used inside kanban columns.
 *
 * <p>Shows the card title, description and due date. Clicking the card
 * (single or double click) opens the card editor through the provided
 * callback. All visual styling lives in card.css; the height is fixed so
 * every card is the same size: the title is truncated to two lines and
 * the description is clipped to a fixed viewport. The full content is
 * available in the card editor dialog.</p>
 */
public class CardView extends VBox {

    /** Maximum number of lines the title may occupy on the board. */
    private static final int TITLE_MAX_LINES = 2;

    /** Content width used to measure title wrapping (card width minus paddings). */
    private static final double TITLE_MEASURE_WIDTH = 232;

    private final Card card;
    private final Consumer<Card> onOpenCard;
    private final MarkdownService markdownService;
    private final BiConsumer<Card, Integer> onTaskToggle;
    private final HostServices hostServices;

    public CardView(Card card, DueDateService dueDateService, MarkdownService markdownService,
                     Consumer<Card> onOpenCard, BiConsumer<Card, Integer> onTaskToggle,
                     HostServices hostServices) {
        this.card = card;
        this.onOpenCard = onOpenCard;
        this.markdownService = markdownService;
        this.onTaskToggle = onTaskToggle;
        this.hostServices = hostServices;

        getStyleClass().add("card");
        setSpacing(4);

        Label titleLabel = new Label(fitTitle(card.getTitle()));
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(titleLabel);

        String description = card.getDescription();
        if (description != null && !description.isBlank()) {
            ScrollPane descScroll = new ScrollPane();
            descScroll.getStyleClass().add("card-desc-scroll");
            descScroll.setFitToWidth(true);
            descScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            descScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Node markdownContent = MarkdownRenderer.render(description, markdownService,
                    taskIndex -> {
                        if (onTaskToggle != null) {
                            onTaskToggle.accept(card, taskIndex);
                        }
                    }, hostServices);
            descScroll.setContent(markdownContent);
            getChildren().add(descScroll);
        }

        // Flexible spacer keeps the due date pinned to the bottom of the card.
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        DueDateStatus dueStatus = dueDateService.status(card.getDueDate(), card.getStatus());
        if (dueStatus != DueDateStatus.NONE) {
            Label dueLabel = new Label(dueDateService.displayText(dueStatus, card.getDueDate()));
            dueLabel.getStyleClass().add("card-due-date");
            String styleClass = dueDateService.styleClass(dueStatus);
            if (styleClass != null) {
                dueLabel.getStyleClass().add(styleClass);
            }
            getChildren().add(dueLabel);
        }

        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        setOnMouseClicked(event -> {
            if (onOpenCard != null) {
                onOpenCard.accept(card);
            }
        });
    }

    public Card getCard() {
        return card;
    }

    /**
     * Truncates the title so it fits in {@link #TITLE_MAX_LINES} lines at the
     * card content width. Cuts at a word boundary and appends an ellipsis.
     */
    private static String fitTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        Font font = Font.font("Segoe UI", FontWeight.BOLD, 14);
        Text lineProbe = new Text("A");
        lineProbe.setFont(font);
        double lineHeight = lineProbe.getLayoutBounds().getHeight();
        double maxHeight = TITLE_MAX_LINES * lineHeight + 0.5;

        int lo = 0;
        int hi = title.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            Text probe = new Text(title.substring(0, mid));
            probe.setFont(font);
            probe.setWrappingWidth(TITLE_MEASURE_WIDTH);
            if (probe.getLayoutBounds().getHeight() <= maxHeight) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        if (lo >= title.length()) {
            return title;
        }
        int cut = title.lastIndexOf(' ', lo);
        if (cut <= 0) {
            cut = lo;
        }
        return title.substring(0, cut) + "\u2026";
    }
}
