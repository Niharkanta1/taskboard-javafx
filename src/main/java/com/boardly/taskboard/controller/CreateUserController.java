package com.boardly.taskboard.controller;

import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.service.AuthService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Modal controller for creating a local TaskBoard user account. */
public class CreateUserController {

    private final AuthService authService;
    private final Stage stage;
    private final Runnable onCreated;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmationField;
    @FXML
    private Label errorLabel;

    public CreateUserController(AuthService authService, Stage stage, Runnable onCreated) {
        this.authService = authService;
        this.stage = stage;
        this.onCreated = onCreated;
    }

    @FXML
    private void onSave() {
        try {
            authService.register(usernameField.getText(), passwordField.getText(), confirmationField.getText());
            if (onCreated != null) {
                onCreated.run();
            }
            stage.close();
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (AppException e) {
            errorLabel.setText("Unable to create the user. Please try again.");
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }
}