package com.example.taskboard;

import com.example.taskboard.config.AppConfig;
import com.example.taskboard.controller.CardDialogController;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.service.MarkdownService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test: CardDialog.fxml must actually load through FXMLLoader
 * with a real controller. ResourceSmokeTest only checks that the file
 * exists and that its imports resolve, which does not catch invalid
 * property bindings (e.g. the read-only {@code fitWidth} property on a
 * {@code ScrollPane}, which must be {@code fitToWidth} instead).
 *
 * <p>
 * The description area must offer Edit/Preview mode buttons (not a
 * side-by-side split view): a ScrollPane whose content is a StackPane
 * holding both the raw Markdown {@code TextArea} and the rendered
 * preview {@code VBox}, with the editor visible in the default mode.
 */
class CardDialogFxmlTest {

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
    void cardDialogFxmlLoads() throws Exception {
        CardDialogController controller = new CardDialogController(
                null, 0L, null, CardStatus.PLANNED, null, null, new MarkdownService(), null);
        FXMLLoader loader = new FXMLLoader(
                CardDialogController.class.getResource(AppConfig.CARD_DIALOG_FXML));
        loader.setController(controller);
        Parent root = loader.load();

        assertTrue(root instanceof BorderPane, "CardDialog root should be a BorderPane");

        // The description area must offer Edit/Preview mode buttons.
        Button editButton = null;
        Button previewButton = null;
        for (Button button : findAll(root, Button.class)) {
            if ("Edit".equals(button.getText())) {
                editButton = button;
            } else if ("Preview".equals(button.getText())) {
                previewButton = button;
            }
        }
        assertNotNull(editButton, "CardDialog must contain an Edit mode button");
        assertNotNull(previewButton, "CardDialog must contain a Preview mode button");

        // The description area must be a single ScrollPane (no SplitPane).
        List<ScrollPane> scrollPanes = findAll(root, ScrollPane.class);
        assertFalse(scrollPanes.isEmpty(), "CardDialog must contain a ScrollPane for the description area");
        ScrollPane scrollPane = scrollPanes.get(0);
        assertNotNull(scrollPane.getContent(), "ScrollPane content must be set");
        assertTrue(scrollPane.getContent() instanceof StackPane,
                "ScrollPane content should be a StackPane holding the editor and the preview");

        StackPane stack = (StackPane) scrollPane.getContent();
        TextArea editor = null;
        VBox preview = null;
        for (Node child : stack.getChildren()) {
            if (child instanceof TextArea textArea) {
                editor = textArea;
            } else if (child instanceof VBox vbox) {
                preview = vbox;
            }
        }
        assertNotNull(editor, "StackPane must contain the Markdown editor TextArea");
        assertNotNull(preview, "StackPane must contain the preview VBox");

        // Default mode is Preview: preview visible, editor hidden.
        assertFalse(editor.isVisible(), "editor must be hidden in the default (preview) mode");
        assertTrue(preview.isVisible(), "preview must be visible in the default (preview) mode");
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
