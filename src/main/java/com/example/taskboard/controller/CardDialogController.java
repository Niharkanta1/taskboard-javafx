package com.example.taskboard.controller;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.service.CardService;
import com.example.taskboard.service.MarkdownService;
import com.example.taskboard.view.MarkdownRenderer;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Modal dialog for creating or editing a card on a board.
 *
 * <p>Validation and persistence are delegated to {@link CardService};
 * the dialog only reports friendly messages for failures.</p>
 */
public class CardDialogController {

    private static final Logger logger = LoggerFactory.getLogger(CardDialogController.class);

    private final CardService cardService;
    private final long boardId;
    private final Card existing;
    private final CardStatus defaultStatus;
    private final Runnable onSaved;
    private final Stage stage;
    private final MarkdownService markdownService;
    private final HostServices hostServices;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField titleField;

    @FXML
    private ChoiceBox<CardStatus> statusChoice;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private TextArea descriptionField;

    @FXML
    private VBox descriptionPreview;

    @FXML
    private Button editButton;

    @FXML
    private Button previewButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Button deleteButton;

    public CardDialogController(CardService cardService,
                              long boardId,
                              Card existing,
                              CardStatus defaultStatus,
                              Runnable onSaved,
                              Stage stage,
                              MarkdownService markdownService,
                              HostServices hostServices) {
        this.cardService = cardService;
        this.boardId = boardId;
        this.existing = existing;
        this.defaultStatus = defaultStatus;
        this.onSaved = onSaved;
        this.stage = stage;
        this.markdownService = markdownService;
        this.hostServices = hostServices;
    }


    @FXML
    private void initialize() {
        statusChoice.getItems().setAll(CardStatus.values());

        if (existing == null) {
            titleLabel.setText("New Card");
            statusChoice.setValue(defaultStatus);
            titleField.requestFocus();
        } else {
            titleLabel.setText("Edit Card");
            titleField.setText(existing.getTitle());
            statusChoice.setValue(existing.getStatus());
            dueDatePicker.setValue(existing.getDueDate());
            descriptionField.setText(existing.getDescription() == null ? "" : existing.getDescription());
            deleteButton.setVisible(true);
            deleteButton.setManaged(true);
        }

        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview());
        refreshPreview();
        setMode(false);
    }

    /**
     * Switches the description area between the raw Markdown editor
     * ({@code preview == false}) and the rendered Markdown preview
     * ({@code preview == true}).
     */
    private void setMode(boolean preview) {
        if (preview) {
            descriptionField.setVisible(false);
            descriptionField.setManaged(false);
            descriptionPreview.setVisible(true);
            descriptionPreview.setManaged(true);
            previewButton.getStyleClass().add("mode-button-active");
            editButton.getStyleClass().remove("mode-button-active");
            refreshPreview();
        } else {
            descriptionField.setVisible(true);
            descriptionField.setManaged(true);
            descriptionPreview.setVisible(false);
            descriptionPreview.setManaged(false);
            editButton.getStyleClass().add("mode-button-active");
            previewButton.getStyleClass().remove("mode-button-active");
        }
    }

    @FXML
    private void onEditMode() {
        setMode(false);
    }

    @FXML
    private void onPreviewMode() {
        setMode(true);
    }

    /**
     * Re-renders the live Markdown preview from the current editor text.
     * Task checkboxes in the preview are not interactive; toggling happens
     * on the board view after the card is saved.
     */
    private void refreshPreview() {
        String text = descriptionField.getText() == null ? "" : descriptionField.getText();
        descriptionPreview.getChildren().clear();
        Node rendered = MarkdownRenderer.render(text, markdownService, null, hostServices);
        descriptionPreview.getChildren().add(rendered);
    }

    @FXML
    private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText();
        CardStatus status = statusChoice.getValue() == null ? defaultStatus : statusChoice.getValue();
        LocalDate dueDate = dueDatePicker.getValue();

        try {
            if (existing == null) {
                cardService.createCard(boardId, title, description, status, dueDate);
            } else {
                cardService.updateCard(existing.getId(), title, description, status, dueDate);
            }
            stage.close();
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            logger.error("Failed to save card", e);
            errorLabel.setText("Unable to save the card. Please try again.");
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    @FXML
    private void onDelete() {
        if (existing == null) {
            return;
        }

        boolean confirmed = DeleteDialogs.confirmDelete(
                stage,
                "Delete Card",
                "Delete card \"" + existing.getTitle() + "\"?");
        if (!confirmed) {
            return;
        }

        try {
            cardService.deleteCard(existing.getId());
            stage.close();
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (AppException e) {
            logger.error("Failed to delete card", e);
            errorLabel.setText("Unable to delete the card. Please try again.");
        }
    }
}
