package com.boardly.taskboard.view;

import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.service.MarkdownService.BlockQuoteBlock;
import com.boardly.taskboard.service.MarkdownService.CodeBlock;
import com.boardly.taskboard.service.MarkdownService.HeadingBlock;
import com.boardly.taskboard.service.MarkdownService.InlineSegment;
import com.boardly.taskboard.service.MarkdownService.ListItemBlock;
import com.boardly.taskboard.service.MarkdownService.MarkdownBlock;
import com.boardly.taskboard.service.MarkdownService.ParagraphBlock;
import com.boardly.taskboard.service.MarkdownService.TableBlock;
import com.boardly.taskboard.service.MarkdownService.TaskItemBlock;

import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * Renders parsed Markdown blocks as plain JavaFX nodes.
 *
 * <p>
 * All content is rendered as text nodes, labels and text areas — no
 * HTML, no scripting. Styling comes from CSS classes in card.css.
 * </p>
 */
public final class MarkdownRenderer {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownRenderer.class);

    /** Left padding in pixels per list nesting level. */
    private static final int INDENT_PIXELS = 16;

    private MarkdownRenderer() {
    }

    /**
     * Renders a card description into a container node.
     *
     * @param markdown        raw Markdown description (may be null or blank)
     * @param markdownService parser for blocks and inline segments
     * @param onTaskToggle    callback invoked with the task index when a
     *                        task-list checkbox is clicked; may be null
     * @param hostServices    used to open http/https links in the default
     *                        browser; may be null (links are still styled)
     */
    public static Node render(String markdown, MarkdownService markdownService,
            Consumer<Integer> onTaskToggle, HostServices hostServices) {
        return render(markdown, markdownService, onTaskToggle, hostServices, null);
    }

    public static Node render(String markdown, MarkdownService markdownService,
            Consumer<Integer> onTaskToggle, HostServices hostServices,
            Function<Long, Optional<Path>> attachmentResolver) {
        return render(markdown, markdownService, onTaskToggle, hostServices, attachmentResolver, 480);
    }

    public static Node render(String markdown, MarkdownService markdownService,
            Consumer<Integer> onTaskToggle, HostServices hostServices,
            Function<Long, Optional<Path>> attachmentResolver,
            double imageMaxWidth) {
        VBox container = new VBox(4);
        container.getStyleClass().add("card-markdown");
        container.setMaxWidth(Double.MAX_VALUE);

        for (MarkdownBlock block : markdownService.parse(markdown)) {
            container.getChildren().add(renderBlock(block, markdownService, onTaskToggle, hostServices,
                    attachmentResolver, imageMaxWidth));
        }
        return container;
    }

    private static Node renderBlock(MarkdownBlock block, MarkdownService markdownService,
            Consumer<Integer> onTaskToggle, HostServices hostServices,
            Function<Long, Optional<Path>> attachmentResolver,
            double imageMaxWidth) {
        switch (block) {
            case HeadingBlock heading: {
                Label label = new Label(heading.text());
                label.getStyleClass().add("card-markdown-heading");
                label.getStyleClass().add("md-h" + Math.min(Math.max(heading.level(), 1), 6));
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                return label;
            }
            case ParagraphBlock paragraph: {
                return inlineFlow(paragraph.text(), markdownService, hostServices, attachmentResolver, imageMaxWidth);
            }
            case ListItemBlock item: {
                HBox row = new HBox(4);
                row.getStyleClass().add("card-markdown-list-item");
                row.setMaxWidth(Double.MAX_VALUE);
                row.setPadding(new Insets(0, 0, 0, INDENT_PIXELS * item.indent()));
                Label bullet = new Label(item.ordered() ? item.number() + "." : "•");
                bullet.getStyleClass().add("md-bullet");
                row.getChildren().add(bullet);
                row.getChildren()
                        .add(inlineFlow(item.text(), markdownService, hostServices, attachmentResolver, imageMaxWidth));
                return row;
            }
            case TaskItemBlock task: {
                HBox row = new HBox(4);
                row.getStyleClass().add("card-markdown-task-item");
                row.setMaxWidth(Double.MAX_VALUE);
                row.setPadding(new Insets(0, 0, 0, INDENT_PIXELS * task.indent()));
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(task.checked());
                // Consume the click so it does not bubble up to the card
                // and open the card editor.
                checkBox.setOnMouseClicked(event -> event.consume());
                if (onTaskToggle != null) {
                    checkBox.setOnAction(event -> onTaskToggle.accept(task.taskIndex()));
                }
                row.getChildren().add(checkBox);
                row.getChildren()
                        .add(inlineFlow(task.text(), markdownService, hostServices, attachmentResolver, imageMaxWidth));
                return row;
            }
            case CodeBlock code: {
                TextArea area = new TextArea(String.join("\n", code.lines()));
                area.setEditable(false);
                area.setWrapText(true);
                area.getStyleClass().add("card-markdown-codeblock");
                area.setMaxWidth(Double.MAX_VALUE);
                return area;
            }
            case TableBlock table: {
                GridPane grid = new GridPane();
                grid.getStyleClass().add("md-table");
                grid.setHgap(8);
                grid.setVgap(4);
                for (int r = 0; r < table.rows().size(); r++) {
                    List<String> cells = table.rows().get(r);
                    for (int c = 0; c < cells.size(); c++) {
                        Label cell = new Label(cells.get(c));
                        cell.setWrapText(true);
                        cell.setMaxWidth(Double.MAX_VALUE);
                        if (r == 0) {
                            cell.getStyleClass().add("md-table-header");
                        }
                        grid.add(cell, c, r);
                    }
                }
                return grid;
            }
            case BlockQuoteBlock quote: {
                VBox quoteBox = new VBox(2);
                quoteBox.getStyleClass().add("md-blockquote");
                quoteBox.setMaxWidth(Double.MAX_VALUE);
                for (MarkdownBlock inner : quote.content()) {
                    quoteBox.getChildren().add(
                            renderBlock(inner, markdownService, onTaskToggle, hostServices, attachmentResolver,
                                    imageMaxWidth));
                }
                return quoteBox;
            }
            default: {
                return new Label("");
            }
        }
    }

    private static TextFlow inlineFlow(String text, MarkdownService markdownService,
            HostServices hostServices, Function<Long, Optional<Path>> attachmentResolver,
            double imageMaxWidth) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("card-markdown-paragraph");
        flow.setMaxWidth(Double.MAX_VALUE);
        for (InlineSegment segment : markdownService.parseInline(text)) {
            if (segment.image()) {
                Node image = imageNode(segment, attachmentResolver, imageMaxWidth);
                if (image != null) {
                    flow.getChildren().add(image);
                    continue;
                }
            }
            flow.getChildren().add(segmentNode(segment, hostServices));
        }
        return flow;
    }

    private static Node imageNode(InlineSegment segment,
            Function<Long, Optional<Path>> resolver, double imageMaxWidth) {
        if (segment.url() == null) {
            return null;
        }
        try {
            javafx.scene.image.Image image;
            if (segment.url().matches("attachment://\\d+")) {
                if (resolver == null) {
                    return null;
                }
                long id = Long.parseLong(segment.url().substring("attachment://".length()));
                Optional<Path> path = resolver.apply(id);
                if (path.isEmpty() || !Files.isRegularFile(path.get())) {
                    return null;
                }
                image = new javafx.scene.image.Image(path.get().toUri().toString(), imageMaxWidth, 0, true, true);
            } else if (segment.url().startsWith("http://") || segment.url().startsWith("https://")) {
                image = new javafx.scene.image.Image(segment.url(), imageMaxWidth, 0, true, true, true);
            } else {
                return null;
            }
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(imageMaxWidth);
            imageView.getStyleClass().add("card-markdown-image");
            return imageView;
        } catch (RuntimeException e) {
            logger.warn("Could not render image attachment", e);
            return null;
        }
    }

    private static Text segmentNode(InlineSegment segment, HostServices hostServices) {
        Text node = new Text(segment.text());
        if (segment.bold()) {
            node.getStyleClass().add("md-bold");
        }
        if (segment.italic()) {
            node.getStyleClass().add("md-italic");
        }
        if (segment.code()) {
            node.getStyleClass().add("md-code");
        }
        if (segment.link()) {
            node.getStyleClass().add("md-link");
            String url = segment.url();
            node.setOnMouseClicked(event -> {
                event.consume();
                openUrl(hostServices, url);
            });
        }
        return node;
    }

    private static void openUrl(HostServices hostServices, String url) {
        if (hostServices == null || url == null || url.isBlank()) {
            return;
        }
        try {
            hostServices.showDocument(url);
        } catch (RuntimeException e) {
            logger.warn("Could not open link {}", url, e);
        }
    }
}
