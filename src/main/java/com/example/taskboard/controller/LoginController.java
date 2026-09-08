package com.example.taskboard.controller;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.exception.AuthenticationException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.User;
import com.example.taskboard.session.SessionManager;
import com.example.taskboard.service.AuthService;
import com.example.taskboard.service.NavigationService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login screen controller.
 *
 * <p>Handles the login form and shows friendly error messages. Business
 * rules live in {@link AuthService}; no SQL or password logic is
 * duplicated here.</p>
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final AuthService authService;
    private final SessionManager sessionManager;
    private final NavigationService navigationService;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    public LoginController(AuthService authService, SessionManager sessionManager,
                           NavigationService navigationService) {
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.navigationService = navigationService;
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        try {
            User user = authService.login(username, password);
            sessionManager.login(user);
            navigationService.showDashboard();
        } catch (ValidationException | AuthenticationException e) {
            showError(e.getMessage());
        } catch (AppException e) {
            logger.error("Login failed due to an application error", e);
            showError("Unable to sign in. Please try again.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}
