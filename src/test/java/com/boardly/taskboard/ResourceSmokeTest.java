package com.boardly.taskboard;

import com.boardly.taskboard.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 0/2/4 smoke tests: verify required resources exist on the classpath
 * and that the basic application configuration is well-formed.
 */
class ResourceSmokeTest {

    private static final Pattern FXML_IMPORT_PATTERN = Pattern.compile("\\?import\\s+([A-Za-z0-9_.]+)\\?");

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
    void workspaceFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.WORKSPACE_FXML),
                "Workspace.fxml must exist on the classpath");
    }

    @Test
    void boardFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.BOARD_FXML),
                "Board.fxml must exist on the classpath");
    }

    @Test
    void boardDialogFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.BOARD_DIALOG_FXML),
                "BoardDialog.fxml must exist on the classpath");
    }

    @Test
    void cardDialogFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.CARD_DIALOG_FXML),
                "CardDialog.fxml must exist on the classpath");
    }

    @Test
    void columnDialogFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.COLUMN_DIALOG_FXML),
                "ColumnDialog.fxml must exist on the classpath");
    }

    @Test
    void tagDialogFxmlExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.TAG_DIALOG_FXML),
                "TagDialog.fxml must exist on the classpath");
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
    void boardCssExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.BOARD_CSS), "board.css must exist on the classpath");
    }

    @Test
    void cardCssExistsOnClasspath() {
        assertNotNull(Main.class.getResource(AppConfig.CARD_CSS), "card.css must exist on the classpath");
    }

    /**
     * Regression test: every {@code <?import ...?>} in every FXML view must
     * reference a class that actually exists. A wrong package (e.g.
     * {@code javafx.scene.layout.ScrollPane} instead of
     * {@code javafx.scene.control.ScrollPane}) makes the view unloadable.
     */
    @Test
    void allFxmlImportsReferenceExistingClasses() throws Exception {
        List<String> fxmlViews = List.of(
                AppConfig.LOGIN_FXML,
                AppConfig.DASHBOARD_FXML,
                AppConfig.WORKSPACE_DIALOG_FXML,
                AppConfig.WORKSPACE_FXML,
                AppConfig.BOARD_FXML,
                AppConfig.BOARD_DIALOG_FXML,
                AppConfig.CARD_DIALOG_FXML,
                AppConfig.COLUMN_DIALOG_FXML,
                AppConfig.TAG_DIALOG_FXML);
        for (String fxml : fxmlViews) {
            URL url = Main.class.getResource(fxml);
            assertNotNull(url, fxml + " must exist on the classpath");
            List<String> imports = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = FXML_IMPORT_PATTERN.matcher(line);
                    if (matcher.find()) {
                        imports.add(matcher.group(1));
                    }
                }
            }
            for (String className : imports) {
                // initialize=false: only verify the class exists, without
                // triggering JavaFX static initializers (toolkit not started in tests)
                Class.forName(className, false, ResourceSmokeTest.class.getClassLoader());
            }
        }
    }

    @Test
    void appConfigurationIsWellFormed() {
        assertTrue(AppConfig.APP_NAME.length() > 0);
        assertTrue(AppConfig.LOGIN_FXML.startsWith("/"));
        assertTrue(AppConfig.DASHBOARD_FXML.startsWith("/"));
        assertTrue(AppConfig.WORKSPACE_DIALOG_FXML.startsWith("/"));
        assertTrue(AppConfig.WORKSPACE_FXML.startsWith("/"));
        assertTrue(AppConfig.BOARD_FXML.startsWith("/"));
        assertTrue(AppConfig.BOARD_DIALOG_FXML.startsWith("/"));
        assertTrue(AppConfig.CARD_DIALOG_FXML.startsWith("/"));
        assertTrue(AppConfig.APP_CSS.startsWith("/"));
        assertTrue(AppConfig.LOGIN_CSS.startsWith("/"));
        assertTrue(AppConfig.DASHBOARD_CSS.startsWith("/"));
        assertTrue(AppConfig.BOARD_CSS.startsWith("/"));
        assertTrue(AppConfig.CARD_CSS.startsWith("/"));
        assertTrue(AppConfig.DEFAULT_WINDOW_WIDTH > 0);
        assertTrue(AppConfig.DEFAULT_WINDOW_HEIGHT > 0);
        assertTrue(AppConfig.BOARD_WINDOW_WIDTH > 0);
        assertTrue(AppConfig.BOARD_WINDOW_HEIGHT > 0);
    }
}
