package com.example.taskboard.controller;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.service.StorageConfigService;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Dialog controller for selecting the persistent database and attachment
 * directory.
 */
public class StorageConfigController {

    private final StorageConfigService storageConfigService;
    private final Stage stage;

    @FXML
    private TextField pathField;

    @FXML
    private Label errorLabel;

    public StorageConfigController(StorageConfigService storageConfigService, Stage stage) {
        this.storageConfigService = storageConfigService;
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        pathField.setText(storageConfigService.getDataDirectory().toString());
    }

    @FXML
    private void onBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose TaskBoard Data Directory");
        chooser.setInitialDirectory(storageConfigService.getDataDirectory().toFile());
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            pathField.setText(selected.toPath().toAbsolutePath().normalize().toString());
        }
    }

    @FXML
    private void onSave() {
        try {
            String value = pathField.getText() == null ? "" : pathField.getText().trim();
            if (value.isBlank()) {
                errorLabel.setText("A data directory is required.");
                return;
            }
            storageConfigService.save(Path.of(value));
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("TaskBoard");
            alert.setHeaderText("Storage location saved");
            alert.setContentText("Restart TaskBoard for the new database and attachments location to take effect.");
            alert.showAndWait();
            stage.close();
        } catch (AppException e) {
            errorLabel.setText("Unable to save this storage location.");
        } catch (RuntimeException e) {
            errorLabel.setText("The selected storage path is invalid.");
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }
}