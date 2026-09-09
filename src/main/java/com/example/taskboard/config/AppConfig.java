package com.example.taskboard.config;

/**
 * Basic, static application configuration.
 *
 * <p>Holds only non-sensitive, compile-time constants. No secrets are
 * stored here and nothing in this class is logged.</p>
 */
public final class AppConfig {

    public static final String APP_NAME = "TaskBoard";
    public static final String APP_VERSION = "0.0.1";

    public static final String LOGIN_FXML = "/fxml/Login.fxml";
    public static final String DASHBOARD_FXML = "/fxml/Dashboard.fxml";
    public static final String WORKSPACE_DIALOG_FXML = "/fxml/WorkspaceDialog.fxml";
    public static final String WORKSPACE_FXML = "/fxml/Workspace.fxml";
    public static final String BOARD_FXML = "/fxml/Board.fxml";
    public static final String BOARD_DIALOG_FXML = "/fxml/BoardDialog.fxml";
    public static final String CARD_DIALOG_FXML = "/fxml/CardDialog.fxml";
    public static final String DELETE_DIALOG_FXML = "/fxml/DeleteDialog.fxml";
    public static final String APP_CSS = "/css/app.css";
    public static final String LOGIN_CSS = "/css/login.css";
    public static final String DASHBOARD_CSS = "/css/dashboard.css";
    public static final String BOARD_CSS = "/css/board.css";
    public static final String CARD_CSS = "/css/card.css";

    public static final int DEFAULT_WINDOW_WIDTH = 600;
    public static final int DEFAULT_WINDOW_HEIGHT = 400;

    public static final int BOARD_WINDOW_WIDTH = 1100;
    public static final int BOARD_WINDOW_HEIGHT = 650;

    public static final int WORKSPACE_DIALOG_WIDTH = 420;
    public static final int WORKSPACE_DIALOG_HEIGHT = 360;

    public static final int BOARD_DIALOG_WIDTH = 420;
    public static final int BOARD_DIALOG_HEIGHT = 360;

    public static final int CARD_DIALOG_WIDTH = 720;
    public static final int CARD_DIALOG_HEIGHT = 560;

    public static final int DELETE_DIALOG_WIDTH = 420;
    public static final int DELETE_DIALOG_HEIGHT = 240;

    private AppConfig() {
    }
}
