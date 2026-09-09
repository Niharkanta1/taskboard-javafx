package com.example.taskboard.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Modal dialog for confirming a destructive action (deleting a workspace,
 * board, or card).
 *
 * <p>Styled to match the rest of the application; callers should use
 * {@link DeleteDialogs#confirmDelete} to show it.</p>
 */
public class DeleteDialogController {

    private final Stage stage;
    private final String title;
    private final String message;
    private boolean confirmed;

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    public DeleteDialogController(String title, String message, Stage stage) {
        this.title = title;
        this.message = message;
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        titleLabel.setText(title);
        messageLabel.setText(message);
    }

    /**
     * @return true if the user confirmed the deletion
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void onDelete() {
        confirmed = true;
        stage.close();
    }

    @FXML
    private void onCancel() {
        stage.close();
    }
}
