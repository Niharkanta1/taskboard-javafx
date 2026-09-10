package com.boardly.taskboard.controller;

import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.service.BoardService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal dialog for creating or editing a board inside a workspace.
 *
 * <p>Validation and persistence are delegated to {@link BoardService};
 * the dialog only reports friendly messages for failures.</p>
 */
public class BoardDialogController {

    private static final Logger logger = LoggerFactory.getLogger(BoardDialogController.class);

    private final BoardService boardService;
    private final long workspaceId;
    private final Board existing;
    private final Runnable onSaved;
    private final Stage stage;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Label errorLabel;

    public BoardDialogController(BoardService boardService,
                                long workspaceId,
                                Board existing,
                                Runnable onSaved,
                                Stage stage) {
        this.boardService = boardService;
        this.workspaceId = workspaceId;
        this.existing = existing;
        this.onSaved = onSaved;
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        if (existing == null) {
            titleLabel.setText("Create Board");
        } else {
            titleLabel.setText("Edit Board");
            nameField.setText(existing.getName());
            descriptionField.setText(existing.getDescription() == null ? "" : existing.getDescription());
        }
        nameField.requestFocus();
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText();

        try {
            if (existing == null) {
                boardService.createBoard(workspaceId, name, description);
            } else {
                boardService.updateBoard(existing.getId(), name, description);
            }
            stage.close();
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            logger.error("Failed to save board", e);
            errorLabel.setText("Unable to save the board. Please try again.");
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }
}
