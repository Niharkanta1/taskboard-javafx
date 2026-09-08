package com.example.taskboard.service;

import com.example.taskboard.config.AppConfig;
import com.example.taskboard.controller.DashboardController;
import com.example.taskboard.controller.LoginController;
import com.example.taskboard.session.SessionManager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Central navigation between application screens.
 *
 * <p>Owns scene creation and stylesheet loading so controllers do not
 * build screens themselves. The session is preserved across screen
 * switches; logout returns to the login screen.</p>
 */
public class NavigationService {

    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);

    private final Stage stage;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final WorkspaceService workspaceService;

    public NavigationService(Stage stage, AuthService authService, SessionManager sessionManager,
                            WorkspaceService workspaceService) {
        this.stage = stage;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.workspaceService = workspaceService;
    }

    /**
     * Returns the primary stage, e.g. for setting modal dialog owners.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Shows the login screen.
     */
    public void showLogin() {
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.LOGIN_FXML));
        loader.setController(new LoginController(authService, sessionManager, this));
        Parent root = loadRoot(loader, AppConfig.LOGIN_FXML);
        showScene(root, AppConfig.LOGIN_CSS);
    }

    /**
     * Shows the dashboard for the currently signed-in user.
     */
    public void showDashboard() {
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.DASHBOARD_FXML));
        loader.setController(new DashboardController(sessionManager, workspaceService, this));
        Parent root = loadRoot(loader, AppConfig.DASHBOARD_FXML);
        showScene(root, AppConfig.DASHBOARD_CSS);
    }

    private Parent loadRoot(FXMLLoader loader, String fxmlPath) {
        try {
            Parent root = loader.load();
            if (root == null) {
                throw new IllegalStateException("FXML document did not produce a root node: " + fxmlPath);
            }
            return root;
        } catch (Exception e) {
            logger.error("Failed to load FXML view: {}", fxmlPath, e);
            throw new IllegalStateException("Failed to load FXML view: " + fxmlPath, e);
        }
    }

    private void showScene(Parent root, String viewCss) {
        Scene scene = new Scene(root, AppConfig.DEFAULT_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_HEIGHT);
        scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
        scene.getStylesheets().add(resolveResource(viewCss).toExternalForm());
        stage.setTitle(AppConfig.APP_NAME);
        stage.setScene(scene);
        stage.show();
    }

    private URL resolveResource(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Required resource not found on classpath: " + resourcePath);
        }
        return url;
    }
}
