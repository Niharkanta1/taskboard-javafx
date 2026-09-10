package com.boardly.taskboard;

import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.view.MarkdownRenderer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the project's MARKDOWN_TEST.md document and asserts that every
 * feature it exercises — headings (h1..h6), emphasis, nested lists,
 * tables, links, nested block quotes, fenced code blocks (incl. mermaid),
 * inline code and image alt text — produces the expected JavaFX nodes.
 */
class MarkdownRenderTest {

    private static final MarkdownService SERVICE = new MarkdownService();

    @BeforeAll
    static void startToolkit() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // Toolkit already initialized by another test class in the same JVM.
        }
    }

    @Test
    void rendersMarkdownTestDocument() throws Exception {
        String markdown = Files.readString(Path.of("MARKDOWN_TEST.md"));
        Node root = MarkdownRenderer.render(markdown, SERVICE, null, null);

        // Headings keep their level (h1 and h6 must not be clamped to h3).
        assertTrue(hasLabel(root, "Markdown syntax guide", "md-h1"), "h1 heading missing");
        assertTrue(hasLabel(root, "This is a Heading h2", "md-h2"), "h2 heading missing");
        assertTrue(hasLabel(root, "This is a Heading h6", "md-h6"), "h6 heading missing");

        // Emphasis.
        assertTrue(hasText(root, "This text will be bold", "md-bold"), "bold text missing");
        assertTrue(hasText(root, "This text will be italic", "md-italic"), "italic text missing");
        assertTrue(hasText(root, "can", "md-bold", "md-italic"), "combined bold/italic missing");

        // Nested list items must be present and indented.
        HBox topRow = rowContaining(root, "Item 1");
        assertNotNull(topRow, "top-level list item missing");
        assertEquals(0, topRow.getPadding().getLeft(), "top-level list items must not be indented");
        HBox nestedRow = rowContaining(root, "Item 3a");
        assertNotNull(nestedRow, "nested list item missing (nested lists were dropped)");
        assertTrue(nestedRow.getPadding().getLeft() > 0, "nested list items must be indented");
        assertNotNull(rowContaining(root, "Item 3b"), "second nested list item missing");

        // Tables render as a GridPane with 4 rows x 2 columns.
        GridPane table = findTable(root);
        assertNotNull(table, "table must render as a GridPane");
        List<Label> cells = findAll(table, Label.class);
        assertEquals(8, cells.size(), "table must have 8 cells (4 rows x 2)");
        assertTrue(cells.stream().anyMatch(c -> c.getStyleClass().contains("md-table-header")),
                "table header row must be styled");

        // Fenced code blocks (plain and mermaid) render as read-only TextAreas.
        List<TextArea> codeAreas = findAll(root, TextArea.class);
        assertTrue(codeAreas.stream().anyMatch(a -> a.getText() != null
                && a.getText().contains("let message = 'Hello world';")),
                "fenced code block missing");
        assertTrue(codeAreas.stream().anyMatch(a -> a.getText() != null && a.getText().contains("graph TD")),
                "mermaid fenced code block missing");

        // Only http/https links render as links.
        assertTrue(hasText(root, "Markdown Live Preview", "md-link"), "https link missing");

        // Nested block quotes render as nested quote boxes.
        List<VBox> quotes = findAll(root, VBox.class).stream()
                .filter(v -> v.getStyleClass().contains("md-blockquote"))
                .toList();
        assertTrue(quotes.size() >= 2, "nested block quotes must render as nested quote boxes");
        boolean nestedInsideOuter = false;
        for (Node child : quotes.get(0).getChildren()) {
            if (child instanceof VBox inner && inner.getStyleClass().contains("md-blockquote")) {
                nestedInsideOuter = true;
            }
        }
        assertTrue(nestedInsideOuter, "nested block quote must be inside the outer quote box");

        // Images render as their alt text (never as file access).
        assertTrue(hasText(root, "This is an alt text."), "image alt text missing");

        // Inline code.
        assertTrue(hasText(root, "markedjs/marked", "md-code"), "inline code missing");
    }

    private static boolean hasLabel(Node root, String text, String styleClass) {
        return findAll(root, Label.class).stream()
                .anyMatch(label -> label.getText() != null && label.getText().equals(text)
                        && label.getStyleClass().contains(styleClass));
    }

    private static boolean hasText(Node root, String text, String... styleClasses) {
        for (Text node : findAll(root, Text.class)) {
            if (node.getText() == null || !node.getText().equals(text)) {
                continue;
            }
            boolean all = true;
            for (String styleClass : styleClasses) {
                if (!node.getStyleClass().contains(styleClass)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    private static HBox rowContaining(Node root, String text) {
        for (HBox row : findAll(root, HBox.class)) {
            if (containsText(row, text)) {
                return row;
            }
        }
        return null;
    }

    private static boolean containsText(Node node, String text) {
        if (node instanceof Text textNode && text.equals(textNode.getText())) {
            return true;
        }
        if (node instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                if (containsText(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static GridPane findTable(Node root) {
        for (GridPane grid : findAll(root, GridPane.class)) {
            if (grid.getStyleClass().contains("md-table")) {
                return grid;
            }
        }
        return null;
    }

    /**
     * Finds nodes of the given type. Only descends through {@code Pane}
     * nodes, because {@code Parent.getChildren()} is protected for
     * non-Pane regions; those are located by type match on the way down.
     */
    private static <T extends Node> List<T> findAll(Node node, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(node)) {
            result.add(type.cast(node));
        }
        if (node instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                result.addAll(findAll(child, type));
            }
        }
        return result;
    }
}
