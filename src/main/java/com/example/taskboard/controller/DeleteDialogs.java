package com.example.taskboard.controller;

import com.example.taskboard.config.AppConfig;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Shows a styled, modal delete-confirmation dialog that matches the
 * application theme, instead of the platform-default {@link javafx.scene.control.Alert}.
 */
public final class DeleteDialogs {

    private DeleteDialogs() {
    }

    /**
     * Shows a modal "delete" confirmation dialog and blocks until the
     * user chooses.
     *
     * @param owner   the stage that should own the dialog
     * @param title   dialog title, e.g. {@code "Delete Workspace"}
     * @param message the confirmation message
     * @return true if the user confirmed the deletion, false otherwise
     */
    public static boolean confirmDelete(Stage owner, String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(resource(AppConfig.DELETE_DIALOG_FXML));
            Stage dialogStage = new Stage();
            DeleteDialogController controller = new DeleteDialogController(title, message, dialogStage);
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, AppConfig.DELETE_DIALOG_WIDTH, AppConfig.DELETE_DIALOG_HEIGHT);
            scene.getStylesheets().add(resource(AppConfig.APP_CSS).toExternalForm());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(owner);
            dialogStage.setTitle(title);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            return controller.isConfirmed();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to show delete confirmation dialog", e);
        }
    }

    private static URL resource(String resourcePath) {
        URL url = DeleteDialogs.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Required resource not found on classpath: " + resourcePath);
        }
        return url;
    }
}
