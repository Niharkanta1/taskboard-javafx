package com.boardly.taskboard.controller;

import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.service.ColumnService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Modal dialog for creating, renaming, or deleting a column.
 *
 * <p>
 * In delete mode the dialog shows a {@code targetChoice} that lets the
 * user pick which column receives the deleted column's cards.
 * </p>
 */
public class ColumnDialogController {

    private static final Logger logger = LoggerFactory.getLogger(ColumnDialogController.class);

    private final ColumnService columnService;
    private final Stage stage;
    private final long boardId;

    /** null when creating a new column; the column being renamed otherwise. */
    private final BoardColumn existing;

    /** The column being deleted; null when not in delete mode. */
    private final BoardColumn toDelete;

    /** Columns that can receive the deleted column's cards. */
    private final List<BoardColumn> targetColumns;

    private final Runnable onSaved;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private CheckBox finalColumnCheck;

    @FXML
    private Label targetLabel;

    @FXML
    private ChoiceBox<BoardColumn> targetChoice;

    @FXML
    private Label errorLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    public ColumnDialogController(ColumnService columnService, Stage stage, long boardId,
            BoardColumn existing, BoardColumn toDelete,
            List<BoardColumn> targetColumns, Runnable onSaved) {
        this.columnService = columnService;
        this.stage = stage;
        this.boardId = boardId;
        this.existing = existing;
        this.toDelete = toDelete;
        this.targetColumns = targetColumns;
        this.onSaved = onSaved;
    }

    @FXML
    private void initialize() {
        if (existing != null) {
            titleLabel.setText("Rename Column");
            nameField.setText(existing.getName());
            finalColumnCheck.setSelected(existing.isFinal());
        }
        if (toDelete != null) {
            titleLabel.setText("Delete Column");
            if (saveButton != null) {
                saveButton.setText("Delete");
                saveButton.getStyleClass().remove("dialog-save");
                saveButton.getStyleClass().add("dialog-delete");
            }
            nameField.setText(toDelete.getName());
            nameField.setDisable(true);
            finalColumnCheck.setSelected(toDelete.isFinal());
            finalColumnCheck.setDisable(true);
            if (targetColumns != null && !targetColumns.isEmpty()) {
                targetLabel.setVisible(true);
                targetLabel.setManaged(true);
                targetChoice.setVisible(true);
                targetChoice.setManaged(true);
                targetChoice.getItems().setAll(targetColumns);
                targetChoice.setValue(targetColumns.get(0));
            }
        }
        nameField.requestFocus();
    }

    @FXML
    private void onSave() {
        try {
            if (toDelete != null) {
                BoardColumn target = targetChoice.getValue();
                columnService.deleteColumn(toDelete.getId(), target != null ? target.getId() : null);
            } else if (existing != null) {
                columnService.renameColumn(existing.getId(), nameField.getText());
                if (finalColumnCheck.isSelected() != existing.isFinal()) {
                    columnService.setColumnFinal(existing.getId(), finalColumnCheck.isSelected());
                }
            } else {
                columnService.createColumn(boardId, nameField.getText(), finalColumnCheck.isSelected());
            }
            if (stage != null) {
                stage.close();
            }
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            logger.error("Failed to save column", e);
            errorLabel
                    .setText(e.getMessage() != null ? e.getMessage() : "Unable to save the column. Please try again.");
        }
    }

    @FXML
    private void onCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}
