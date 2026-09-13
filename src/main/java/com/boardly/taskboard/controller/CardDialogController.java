package com.boardly.taskboard.controller;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardAttachment;
import com.boardly.taskboard.model.CardPriority;
import com.boardly.taskboard.model.CardSeverity;
import com.boardly.taskboard.model.CardStatus;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.service.AttachmentService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.service.TagService;
import com.boardly.taskboard.view.MarkdownRenderer;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Modal dialog for creating or editing a card.
 */
public class CardDialogController {

    private static final Logger logger = LoggerFactory.getLogger(CardDialogController.class);
    private static final double PREVIEW_IMAGE_WIDTH = 220;

    private final CardService cardService;
    private final TagService tagService;
    private final long boardId;
    private final Card existing;
    private final BoardColumn defaultColumn;
    private final Runnable onSaved;
    private final Stage stage;
    private final MarkdownService markdownService;
    private final HostServices hostServices;
    private final AttachmentService attachmentService;

    private final Set<Long> selectedTagIds = new HashSet<>();
    private boolean saved;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField titleField;

    @FXML
    private ChoiceBox<BoardColumn> columnChoice;

    @FXML
    private ChoiceBox<CardPriority> priorityChoice;

    @FXML
    private ChoiceBox<CardSeverity> severityChoice;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private FlowPane tagsFlowPane;

    @FXML
    private Button manageTagsButton;

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
        this(cardService, null, boardId, existing, defaultColumn, null, stage, markdownService, hostServices, null);
    }

    public CardDialogController(CardService cardService, long boardId, Card existing,
            BoardColumn defaultColumn, HostServices hostServices,
            MarkdownService markdownService, Stage stage,
            AttachmentService attachmentService) {
        this(cardService, null, boardId, existing, defaultColumn, null, stage, markdownService, hostServices,
                attachmentService);
    }

    public CardDialogController(CardService cardService, long boardId, Card existing,
            BoardColumn defaultColumn, Runnable onSaved, Stage stage,
            MarkdownService markdownService, HostServices hostServices,
            AttachmentService attachmentService) {
        this(cardService, null, boardId, existing, defaultColumn, onSaved, stage, markdownService, hostServices,
                attachmentService);
    }

    public CardDialogController(CardService cardService, TagService tagService, long boardId, Card existing,
            BoardColumn defaultColumn, Runnable onSaved, Stage stage,
            MarkdownService markdownService, HostServices hostServices,
            AttachmentService attachmentService) {
        this.cardService = cardService;
        this.tagService = tagService;
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
        this(cardService, null, boardId, existing, null, onSaved, stage, markdownService, hostServices, null);
    }

    @FXML
    private void initialize() {
        populateColumnChoices();

        if (priorityChoice != null) {
            priorityChoice.getItems().setAll(CardPriority.values());
            priorityChoice.setValue(CardPriority.MEDIUM);
        }
        if (severityChoice != null) {
            severityChoice.getItems().setAll(CardSeverity.values());
            severityChoice.setValue(CardSeverity.MINOR);
        }

        if (existing != null) {
            titleLabel.setText("Edit Card");
            titleField.setText(existing.getTitle());
            descriptionField.setText(existing.getDescription() == null ? "" : existing.getDescription());
            dueDatePicker.setValue(existing.getDueDate());
            if (priorityChoice != null) {
                priorityChoice.setValue(existing.getPriority() != null ? existing.getPriority() : CardPriority.MEDIUM);
            }
            if (severityChoice != null) {
                severityChoice.setValue(existing.getSeverity() != null ? existing.getSeverity() : CardSeverity.MINOR);
            }
            if (existing.getTags() != null) {
                for (Tag t : existing.getTags()) {
                    selectedTagIds.add(t.getId());
                }
            }
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

        populateTags();

        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview());
        setMode(true);
    }

    private void populateColumnChoices() {
        if (columnChoice == null)
            return;
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

    private void populateTags() {
        if (tagsFlowPane == null)
            return;
        tagsFlowPane.getChildren().clear();
        if (tagService == null)
            return;

        List<Tag> tags = tagService.getAllTags();
        if (tags.isEmpty()) {
            Label noTagsLabel = new Label("No tags available. Click 'Manage Tags' to create one.");
            noTagsLabel.setStyle("-fx-text-fill: -app-fg-muted; -fx-font-size: 12px;");
            tagsFlowPane.getChildren().add(noTagsLabel);
            return;
        }

        for (Tag tag : tags) {
            CheckBox checkBox = new CheckBox(tag.getName());
            checkBox.setSelected(selectedTagIds.contains(tag.getId()));
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    selectedTagIds.add(tag.getId());
                } else {
                    selectedTagIds.remove(tag.getId());
                }
            });

            Circle dot = new Circle(5);
            try {
                dot.setFill(Color.web(tag.getColor()));
            } catch (Exception ignored) {
                dot.setFill(Color.web("#0ea5e9"));
            }

            HBox tagPill = new HBox(4, dot, checkBox);
            tagPill.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tagPill.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-padding: 2px 8px; -fx-background-radius: 12px; -fx-border-color: #ded8cc; -fx-border-radius: 12px;");
            tagsFlowPane.getChildren().add(tagPill);
        }
    }

    @FXML
    private void onManageTags() {
        if (tagService == null)
            return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AppConfig.TAG_DIALOG_FXML));
            Stage dialogStage = new Stage();
            TagDialogController controller = new TagDialogController(tagService, dialogStage, this::populateTags);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.TAG_DIALOG_WIDTH, AppConfig.TAG_DIALOG_HEIGHT);
            scene.getStylesheets().add(getClass().getResource(AppConfig.APP_CSS).toExternalForm());
            scene.getStylesheets().add(getClass().getResource(AppConfig.BOARD_CSS).toExternalForm());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setTitle("Manage Tags");
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            populateTags();
        } catch (Exception e) {
            logger.error("Failed to open tag dialog", e);
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
        BoardColumn selectedColumn = columnChoice != null ? columnChoice.getValue() : defaultColumn;
        if (selectedColumn == null && cardService != null) {
            List<BoardColumn> columns = cardService.getColumns(boardId);
            if (!columns.isEmpty()) {
                selectedColumn = columns.get(0);
            }
        }
        if (selectedColumn == null) {
            errorLabel.setText("Choose a column for the card.");
            return;
        }
        LocalDate dueDate = dueDatePicker != null ? dueDatePicker.getValue() : null;
        CardPriority priority = priorityChoice != null && priorityChoice.getValue() != null ? priorityChoice.getValue()
                : CardPriority.MEDIUM;
        CardSeverity severity = severityChoice != null && severityChoice.getValue() != null ? severityChoice.getValue()
                : CardSeverity.MINOR;
        List<Long> tagIds = new ArrayList<>(selectedTagIds);

        try {
            if (cardService != null) {
                if (existing == null) {
                    cardService.createCard(boardId, title, description, selectedColumn.getId(), dueDate, priority,
                            severity, tagIds);
                } else {
                    cardService.updateCard(existing.getId(), title, description, selectedColumn.getId(), dueDate,
                            priority, severity, tagIds);
                }
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
            if (cardService != null) {
                cardService.deleteCard(existing.getId());
            }
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
