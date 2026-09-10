package com.example.taskboard.controller;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.service.AuthService;
import com.example.taskboard.service.NavigationService;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Initial user-selection screen shown before credential login. */
public class StartController {

    private static final Logger logger = LoggerFactory.getLogger(StartController.class);

    private final AuthService authService;
    private final NavigationService navigationService;

    @FXML
    private ListView<String> userList;
    @FXML
    private Label errorLabel;
    @FXML
    private Button continueButton;

    public StartController(AuthService authService, NavigationService navigationService) {
        this.authService = authService;
        this.navigationService = navigationService;
    }

    @FXML
    private void initialize() {
        refreshUsers();
        userList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> continueButton.setDisable(newValue == null));
        userList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && userList.getSelectionModel().getSelectedItem() != null) {
                onContinue();
            }
        });
    }

    @FXML
    private void onContinue() {
        String username = userList.getSelectionModel().getSelectedItem();
        if (username != null) {
            navigationService.showLogin(username);
        }
    }

    @FXML
    private void onCreateUser() {
        navigationService.showCreateUser(this::refreshUsers);
    }

    @FXML
    private void onStorageSettings() {
        navigationService.showStorageConfig();
    }

    private void refreshUsers() {
        try {
            userList.getItems().setAll(authService.findUsernames());
            errorLabel.setText("");
        } catch (AppException e) {
            logger.error("Failed to load user list", e);
            errorLabel.setText("Unable to load users. Please try again.");
        }
    }
}