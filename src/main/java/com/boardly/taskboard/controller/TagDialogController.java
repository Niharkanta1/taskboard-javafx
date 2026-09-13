package com.boardly.taskboard.controller;

import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.service.TagService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Controller for managing custom tags (create, rename, recolor, delete).
 */
public class TagDialogController {

    private static final Logger logger = LoggerFactory.getLogger(TagDialogController.class);

    private final TagService tagService;
    private final Stage stage;
    private final Runnable onChanged;

    private Tag editingTag;

    @FXML
    private ListView<Tag> tagsListView;

    @FXML
    private Label formSectionTitle;

    @FXML
    private TextField tagNameField;

    @FXML
    private ColorPicker tagColorPicker;

    @FXML
    private Button cancelEditButton;

    @FXML
    private Button saveTagButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Button closeButton;

    public TagDialogController(TagService tagService, Stage stage, Runnable onChanged) {
        this.tagService = tagService;
        this.stage = stage;
        this.onChanged = onChanged;
    }

    @FXML
    private void initialize() {
        tagColorPicker.setValue(Color.web("#0ea5e9"));
        tagsListView.setCellFactory(listView -> new TagListCell());
        refreshTags();
    }

    private void refreshTags() {
        if (tagService == null) {
            return;
        }
        List<Tag> tags = tagService.getAllTags();
        tagsListView.getItems().setAll(tags);
    }

    @FXML
    private void onSaveTag() {
        String name = tagNameField.getText();
        Color color = tagColorPicker.getValue();
        String hexColor = toHex(color != null ? color : Color.web("#0ea5e9"));

        try {
            if (editingTag == null) {
                tagService.createTag(name, hexColor);
            } else {
                tagService.updateTag(editingTag.getId(), name, hexColor);
            }
            errorLabel.setText("");
            resetForm();
            refreshTags();
            if (onChanged != null) {
                onChanged.run();
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            logger.error("Failed to save tag", e);
            errorLabel.setText("Unable to save the tag. Please try again.");
        }
    }

    @FXML
    private void onCancelEdit() {
        resetForm();
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void resetForm() {
        editingTag = null;
        tagNameField.clear();
        tagColorPicker.setValue(Color.web("#0ea5e9"));
        formSectionTitle.setText("Create New Tag");
        saveTagButton.setText("+ Add Tag");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        errorLabel.setText("");
    }

    private void startEditing(Tag tag) {
        if (tag == null)
            return;
        editingTag = tag;
        tagNameField.setText(tag.getName());
        try {
            tagColorPicker.setValue(Color.web(tag.getColor()));
        } catch (Exception ignored) {
            tagColorPicker.setValue(Color.web("#0ea5e9"));
        }
        formSectionTitle.setText("Edit Tag: " + tag.getName());
        saveTagButton.setText("Save Changes");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        errorLabel.setText("");
        tagNameField.requestFocus();
    }

    private void deleteTag(Tag tag) {
        if (tag == null)
            return;
        try {
            tagService.deleteTag(tag.getId());
            if (editingTag != null && editingTag.getId().equals(tag.getId())) {
                resetForm();
            }
            refreshTags();
            if (onChanged != null) {
                onChanged.run();
            }
        } catch (AppException e) {
            logger.error("Failed to delete tag", e);
            errorLabel.setText("Unable to delete the tag.");
        }
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private class TagListCell extends ListCell<Tag> {
        @Override
        protected void updateItem(Tag tag, boolean empty) {
            super.updateItem(tag, empty);
            if (empty || tag == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Circle colorDot = new Circle(6);
                try {
                    colorDot.setFill(Color.web(tag.getColor()));
                } catch (Exception e) {
                    colorDot.setFill(Color.web("#0ea5e9"));
                }

                Label name = new Label(tag.getName());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button editBtn = new Button("Edit");
                editBtn.getStyleClass().add("column-action-button");
                editBtn.setOnAction(e -> startEditing(tag));

                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().add("column-delete-button");
                deleteBtn.setOnAction(e -> deleteTag(tag));

                row.getChildren().addAll(colorDot, name, spacer, editBtn, deleteBtn);
                setText(null);
                setGraphic(row);
            }
        }
    }
}
