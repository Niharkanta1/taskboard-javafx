package com.example.taskboard.controller;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.service.WorkspaceService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal dialog for creating or editing a workspace.
 *
 * <p>Validation and persistence are delegated to {@link WorkspaceService};
 * the dialog only reports friendly messages for failures.</p>
 */
public class WorkspaceDialogController {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceDialogController.class);

    private final WorkspaceService workspaceService;
    private final Workspace existing;
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

    public WorkspaceDialogController(WorkspaceService workspaceService,
                                    Workspace existing,
                                    Runnable onSaved,
                                    Stage stage) {
        this.workspaceService = workspaceService;
        this.existing = existing;
        this.onSaved = onSaved;
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        if (existing == null) {
            titleLabel.setText("Create Workspace");
        } else {
            titleLabel.setText("Edit Workspace");
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
                workspaceService.create(name, description);
            } else {
                workspaceService.update(existing.getId(), name, description);
            }
            stage.close();
            if (onSaved != null) {
                onSaved.run();
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            logger.error("Failed to save workspace", e);
            errorLabel.setText("Unable to save the workspace. Please try again.");
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }
}
