package com.example.taskboard;

import com.example.taskboard.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 0/2 smoke tests: verify required resources exist on the classpath
 * and that the basic application configuration is well-formed.
 */
class ResourceSmokeTest {

    @Test
    void loginFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.LOGIN_FXML), "Login.fxml must exist on the classpath");
    }

    @Test
    void dashboardFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.DASHBOARD_FXML), "Dashboard.fxml must exist on the classpath");
    }

    @Test
    void workspaceDialogFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.WORKSPACE_DIALOG_FXML),
                "WorkspaceDialog.fxml must exist on the classpath");
    }

    @Test
    void appCssExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.APP_CSS), "app.css must exist on the classpath");
    }

    @Test
    void loginCssExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.LOGIN_CSS), "login.css must exist on the classpath");
    }

    @Test
    void dashboardCssExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.DASHBOARD_CSS), "dashboard.css must exist on the classpath");
    }

    @Test
    void appConfigurationIsWellFormed() {
        assertTrue(AppConfig.APP_NAME.length() > 0);
        assertTrue(AppConfig.LOGIN_FXML.startsWith("/"));
        assertTrue(AppConfig.DASHBOARD_FXML.startsWith("/"));
        assertTrue(AppConfig.WORKSPACE_DIALOG_FXML.startsWith("/"));
        assertTrue(AppConfig.APP_CSS.startsWith("/"));
        assertTrue(AppConfig.LOGIN_CSS.startsWith("/"));
        assertTrue(AppConfig.DASHBOARD_CSS.startsWith("/"));
        assertTrue(AppConfig.DEFAULT_WINDOW_WIDTH > 0);
        assertTrue(AppConfig.DEFAULT_WINDOW_HEIGHT > 0);
    }
}
