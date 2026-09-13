package com.boardly.taskboard.controller;

import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardAttachment;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.service.AttachmentService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.view.MarkdownRenderer;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Modal dialog for creating or editing a card.
 *
 * <p>
 * The card's column is chosen from the board's dynamic columns instead of
 * a fixed status.
 * </p>
 */
public class CardDialogController {

    private static final Logger logger = LoggerFactory.getLogger(CardDialogController.class);
    private static final double PREVIEW_IMAGE_WIDTH = 220;

    private final CardService cardService;
    private final long boardId;
    private final Card existing;
    private final BoardColumn defaultColumn;
    private final Runnable onSaved;
    private final Stage stage;
    private final MarkdownService markdownService;
    private final HostServices hostServices;
    private final AttachmentService attachmentService;

    private boolean saved;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField titleField;

    @FXML
    private ChoiceBox<BoardColumn> columnChoice;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private Button editButton;

    @FXML
    private Button previewButton;

    @FXML
    private Button attachButton;

    @FXML
    private TextArea descriptionField;

    @FXML
    private VBox descriptionPreview;

    @FXML
    private Label errorLabel;

    @FXML
    private Button deleteButton;

    public CardDialogController(CardService cardService, long boardId, Card existing,
            BoardColumn defaultColumn, HostServices hostServices,
            MarkdownService markdownService, Stage stage) {
        this(cardService, boardId, existing, defaultColumn, null, stage, markdownService, hostServices, null);
    }

    public CardDialogController(CardService cardService, long boardId, Card existing,
            BoardColumn defaultColumn, HostServices hostServices,
            MarkdownService markdownService, Stage stage,
            AttachmentService attachmentService) {
        this(cardService, boardId, existing, defaultColumn, null, stage, markdownService, hostServices,
                attachmentService);
    }

    public CardDialogController(CardService cardService, long boardId, Card existing,
            BoardColumn defaultColumn, Runnable onSaved, Stage stage,
            MarkdownService markdownService, HostServices hostServices,
            AttachmentService attachmentService) {
        this.cardService = cardService;
        this.boardId = boardId;
        this.existing = existing;
        this.defaultColumn = defaultColumn;
        this.onSaved = onSaved;
        this.stage = stage;
        this.markdownService = markdownService;
        this.hostServices = hostServices;
        this.attachmentService = attachmentService;
    }

    /** Legacy constructor support for tests. */
    public CardDialogController(CardService cardService, long boardId, Card existing,
            CardStatus status, Runnable onSaved, Stage stage,
            MarkdownService markdownService, HostServices hostServices) {
        this(cardService, boardId, existing, null, onSaved, stage, markdownService, hostServices, null);
    }

    @FXML
    private void initialize() {
        populateColumnChoices();
        if (existing != null) {
            titleLabel.setText("Edit Card");
            titleField.setText(existing.getTitle());
            descriptionField.setText(existing.getDescription() == null ? "" : existing.getDescription());
            dueDatePicker.setValue(existing.getDueDate());
            deleteButton.setVisible(true);
            deleteButton.setManaged(true);
            attachButton.setDisable(attachmentService == null);
        } else {
            titleLabel.setText("New Card");
            if (defaultColumn != null) {
                columnChoice.setValue(defaultColumn);
            }
            attachButton.setDisable(true);
        }

        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview());
        setMode(true);
    }

    private void populateColumnChoices() {
        columnChoice.getItems().clear();
        if (cardService != null && boardId > 0) {
            List<BoardColumn> columns = cardService.getColumns(boardId);
            columnChoice.getItems().addAll(columns);
            if (existing != null) {
                for (BoardColumn col : columns) {
                    if (col.getId() != null && col.getId() == existing.getBoardColumnId()) {
                        columnChoice.setValue(col);
                        break;
                    }
                }
            } else if (defaultColumn != null) {
                columnChoice.setValue(defaultColumn);
            } else if (!columns.isEmpty()) {
                columnChoice.setValue(columns.get(0));
            }
        }
    }

    private void setMode(boolean preview) {
        if (preview) {
            descriptionField.setVisible(false);
            descriptionField.setManaged(false);
            descriptionPreview.setVisible(true);
            descriptionPreview.setManaged(true);
            if (previewButton != null) {
                previewButton.getStyleClass().add("mode-button-active");
            }
            if (editButton != null) {
                editButton.getStyleClass().remove("mode-button-active");
            }
            refreshPreview();
        } else {
            descriptionField.setVisible(true);
            descriptionField.setManaged(true);
            descriptionPreview.setVisible(false);
            descriptionPreview.setManaged(false);
            if (editButton != null) {
                editButton.getStyleClass().add("mode-button-active");
            }
            if (previewButton != null) {
                previewButton.getStyleClass().remove("mode-button-active");
            }
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

    private void refreshPreview() {
        if (markdownService == null) {
            return;
        }
        String text = descriptionField.getText() == null ? "" : descriptionField.getText();
        descriptionPreview.getChildren().clear();
        Node rendered = MarkdownRenderer.render(text, markdownService, null, hostServices,
                attachmentService == null ? null : attachmentService::resolveById, PREVIEW_IMAGE_WIDTH);
        descriptionPreview.getChildren().add(rendered);
    }

    @FXML
    private void onAttachImage() {
        if (existing == null || attachmentService == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images (PNG, JPG, JPEG)", "*.png", "*.PNG", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG"));
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }
        try {
            CardAttachment attachment = attachmentService.attachImage(existing.getId(), Path.of(selected.toURI()));
            String alt = attachment.getFileName().replace("]", "");
            String current = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
            String reference = "![" + alt + "](attachment://" + attachment.getId() + ")";
            descriptionField.setText(current.isEmpty() ? reference : current + "\n\n" + reference);
            errorLabel.setText("");
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            logger.error("Failed to attach image to card {}", existing.getId(), e);
            errorLabel.setText("Unable to attach the image. Please try again.");
        }
    }

    @FXML
    private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            errorLabel.setText("Card title must not be empty.");
            return;
        }
        String description = descriptionField.getText();
        BoardColumn selectedColumn = columnChoice.getValue();
        if (selectedColumn == null) {
            errorLabel.setText("Choose a column for the card.");
            return;
        }
        LocalDate dueDate = dueDatePicker.getValue();

        try {
            if (existing == null) {
                cardService.createCard(boardId, title, description, selectedColumn.getId(), dueDate);
            } else {
                cardService.updateCard(existing.getId(), title, description, selectedColumn.getId(), dueDate);
            }
            saved = true;
            if (stage != null) {
                stage.close();
            }
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
        if (stage != null) {
            stage.close();
        }
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
            saved = true;
            if (stage != null) {
                stage.close();
            }
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (AppException e) {
            logger.error("Failed to delete card", e);
            errorLabel.setText("Unable to delete the card. Please try again.");
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
