package com.boardly.taskboard.view;

import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardPriority;
import com.boardly.taskboard.model.CardSeverity;
import com.boardly.taskboard.model.DueDateStatus;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.service.DueDateService;
import com.boardly.taskboard.service.MarkdownService;

import javafx.application.HostServices;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Optional;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Reusable card component used inside kanban columns.
 *
 * <p>
 * Shows the card title, priority, severity, tags, description and due date.
 * Clicking the card opens the card editor through the provided callback.
 * </p>
 */
public class CardView extends VBox {

    private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    /** Maximum number of lines the title may occupy on the board. */
    private static final int TITLE_MAX_LINES = 2;

    /** Content width used to measure title wrapping (card width minus paddings). */
    private static final double TITLE_MEASURE_WIDTH = 232;

    private final Card card;
    private final Consumer<Card> onOpenCard;
    private final MarkdownService markdownService;
    private final BiConsumer<Card, Integer> onTaskToggle;
    private final HostServices hostServices;

    /**
     * Simple constructor used by the board view: no task toggling, no
     * attachment resolution.
     */
    public CardView(Card card, MarkdownService markdownService, DueDateService dueDateService,
            Consumer<Card> onOpenCard) {
        this(card, dueDateService, markdownService, onOpenCard, null, null, null, 0);
    }

    public CardView(Card card, DueDateService dueDateService, MarkdownService markdownService,
            Consumer<Card> onOpenCard, BiConsumer<Card, Integer> onTaskToggle,
            HostServices hostServices) {
        this(card, dueDateService, markdownService, onOpenCard, onTaskToggle, hostServices, null);
    }

    public CardView(Card card, DueDateService dueDateService, MarkdownService markdownService,
            Consumer<Card> onOpenCard, BiConsumer<Card, Integer> onTaskToggle,
            HostServices hostServices, Function<Long, Optional<Path>> attachmentResolver) {
        this(card, dueDateService, markdownService, onOpenCard, onTaskToggle, hostServices,
                attachmentResolver, 0);
    }

    public CardView(Card card, DueDateService dueDateService, MarkdownService markdownService,
            Consumer<Card> onOpenCard, BiConsumer<Card, Integer> onTaskToggle,
            HostServices hostServices, Function<Long, Optional<Path>> attachmentResolver,
            int attachmentCount) {
        this.card = card;
        this.onOpenCard = onOpenCard;
        this.markdownService = markdownService;
        this.onTaskToggle = onTaskToggle;
        this.hostServices = hostServices;

        getStyleClass().add("card");
        setSpacing(4);

        // Top Row: Priority Badge, Severity Badge, Spacer, Created Date
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        CardPriority priority = card.getPriority() != null ? card.getPriority() : CardPriority.MEDIUM;
        Label priorityLabel = new Label(priority.getDisplayName());
        priorityLabel.getStyleClass().addAll("card-badge", "priority-" + priority.name().toLowerCase(Locale.ROOT));

        CardSeverity severity = card.getSeverity() != null ? card.getSeverity() : CardSeverity.MINOR;
        Label severityLabel = new Label(severity.getDisplayName());
        severityLabel.getStyleClass().addAll("card-badge", "severity-" + severity.name().toLowerCase(Locale.ROOT));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        String createdText = "";
        if (card.getCreatedAt() != null) {
            createdText = CREATED_FORMAT.format(card.getCreatedAt().atZone(ZoneId.systemDefault()));
        }
        Label createdLabel = new Label(createdText);
        createdLabel.getStyleClass().add("card-created-date");

        topRow.getChildren().addAll(priorityLabel, severityLabel, topSpacer, createdLabel);
        getChildren().add(topRow);

        // Title
        Label titleLabel = new Label(fitTitle(card.getTitle()));
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(titleLabel);

        // Tags row
        if (card.getTags() != null && !card.getTags().isEmpty()) {
            FlowPane tagsPane = new FlowPane();
            tagsPane.setHgap(4);
            tagsPane.setVgap(4);
            for (Tag tag : card.getTags()) {
                Label tagLabel = new Label(tag.getName());
                tagLabel.getStyleClass().add("card-tag");
                String color = tag.getColor() != null ? tag.getColor() : "#0ea5e9";
                tagLabel.setStyle("-fx-background-color: " + color + ";");
                tagsPane.getChildren().add(tagLabel);
            }
            getChildren().add(tagsPane);
        }

        // Markdown Description Preview
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
                    }, hostServices, attachmentResolver);
            descScroll.setContent(markdownContent);
            getChildren().add(descScroll);
        }

        // Flexible spacer keeps the bottom metadata pinned
        Region spacer = new Region();
        spacer.setMinHeight(4);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        // Bottom Row: Due date & attachment count
        DueDateStatus dueStatus = dueDateService.status(card.getDueDate(), card.isInFinalColumn());
        if (dueStatus != DueDateStatus.NONE || attachmentCount > 0) {
            HBox metadata = new HBox(8);
            metadata.setAlignment(Pos.CENTER_LEFT);
            metadata.setMaxWidth(Double.MAX_VALUE);
            Region metadataSpacer = new Region();
            HBox.setHgrow(metadataSpacer, Priority.ALWAYS);

            Label dueLabel = new Label(dueDateService.displayText(dueStatus, card.getDueDate()));
            if (dueStatus != DueDateStatus.NONE) {
                dueLabel.getStyleClass().add("card-due-date");
                String styleClass = dueDateService.styleClass(dueStatus);
                if (styleClass != null) {
                    dueLabel.getStyleClass().add(styleClass);
                }
                metadata.getChildren().add(dueLabel);
            }
            metadata.getChildren().add(metadataSpacer);
            if (attachmentCount > 0) {
                Label attachmentLabel = new Label("Attachments: " + attachmentCount);
                attachmentLabel.getStyleClass().add("card-attachment-count");
                metadata.getChildren().add(attachmentLabel);
            }
            getChildren().add(metadata);
        }

        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        setOnMouseClicked(event -> {
            if (onOpenCard != null) {
                onOpenCard.accept(card);
            }
        });

        // Cards are drag sources; the column views handle the drop.
        setOnDragDetected(event -> {
            javafx.scene.input.Dragboard db = startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString("card:" + card.getId());
            db.setContent(content);
            event.consume();
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
