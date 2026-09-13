package com.boardly.taskboard.service;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.controller.BoardController;
import com.boardly.taskboard.controller.DashboardController;
import com.boardly.taskboard.controller.LoginController;
import com.boardly.taskboard.controller.StorageConfigController;
import com.boardly.taskboard.controller.CreateUserController;
import com.boardly.taskboard.controller.StartController;
import com.boardly.taskboard.controller.WorkspaceController;
import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.session.SessionManager;

import javafx.application.HostServices;
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
 * <p>
 * Owns scene creation and stylesheet loading so controllers do not
 * build screens themselves. The session is preserved across screen
 * switches; logout returns to the login screen.
 * </p>
 */
public class NavigationService {

    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);

    private final Stage stage;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final WorkspaceService workspaceService;
    private final BoardService boardService;
    private final CardService cardService;
    private final ColumnService columnService;
    private final AttachmentService attachmentService;
    private final DueDateService dueDateService;
    private final MarkdownService markdownService;
    private final HostServices hostServices;
    private final StorageConfigService storageConfigService;

    public NavigationService(Stage stage, AuthService authService, SessionManager sessionManager,
            WorkspaceService workspaceService, BoardService boardService,
            CardService cardService, ColumnService columnService,
            AttachmentService attachmentService,
            DueDateService dueDateService,
            MarkdownService markdownService, HostServices hostServices,
            StorageConfigService storageConfigService) {
        this.stage = stage;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.workspaceService = workspaceService;
        this.boardService = boardService;
        this.cardService = cardService;
        this.columnService = columnService;
        this.attachmentService = attachmentService;
        this.dueDateService = dueDateService;
        this.markdownService = markdownService;
        this.hostServices = hostServices;
        this.storageConfigService = storageConfigService;
    }

    public ColumnService getColumnService() {
        return columnService;
    }

    public AttachmentService getAttachmentService() {
        return attachmentService;
    }

    public DueDateService getDueDateService() {
        return dueDateService;
    }

    public MarkdownService getMarkdownService() {
        return markdownService;
    }

    public HostServices getHostServices() {
        return hostServices;
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
        showLogin(null);
    }

    public void showLogin(String username) {
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.LOGIN_FXML));
        loader.setController(new LoginController(authService, sessionManager, this, username));
        Parent root = loadRoot(loader, AppConfig.LOGIN_FXML);
        showScene(root, AppConfig.LOGIN_CSS);
    }

    public void showStart() {
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.START_FXML));
        loader.setController(new StartController(authService, this));
        Parent root = loadRoot(loader, AppConfig.START_FXML);
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

    public void showStorageConfig() {
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.STORAGE_CONFIG_FXML));
        Stage dialogStage = new Stage();
        loader.setController(new StorageConfigController(storageConfigService, dialogStage));
        Parent root = loadRoot(loader, AppConfig.STORAGE_CONFIG_FXML);
        Scene scene = new Scene(root, 620, 260);
        scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
        scene.getStylesheets().add(resolveResource(AppConfig.DASHBOARD_CSS).toExternalForm());
        dialogStage.setTitle("Storage Settings");
        dialogStage.setScene(scene);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.showAndWait();
    }

    public void showCreateUser() {
        showCreateUser(null);
    }

    public void showCreateUser(Runnable onCreated) {
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.CREATE_USER_FXML));
        Stage dialogStage = new Stage();
        loader.setController(new CreateUserController(authService, dialogStage, onCreated));
        Parent root = loadRoot(loader, AppConfig.CREATE_USER_FXML);
        Scene scene = new Scene(root, 440, 430);
        scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
        scene.getStylesheets().add(resolveResource(AppConfig.LOGIN_CSS).toExternalForm());
        dialogStage.setTitle("Create User");
        dialogStage.setScene(scene);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.showAndWait();
    }

    /**
     * Shows the workspace view (board list) for the given workspace.
     */
    public void showWorkspace(Workspace workspace) {
        long currentUserId = sessionManager.getCurrentUser()
                .orElseThrow(() -> new AppException("No signed-in user."))
                .getId();
        if (workspace == null || workspace.getOwnerUserId() != currentUserId) {
            logger.warn("Blocked workspace navigation for user {}", currentUserId);
            showDashboard();
            return;
        }
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.WORKSPACE_FXML));
        loader.setController(new WorkspaceController(workspace, boardService, this));
        Parent root = loadRoot(loader, AppConfig.WORKSPACE_FXML);
        showScene(root, AppConfig.DASHBOARD_CSS, AppConfig.BOARD_CSS);
    }

    /**
     * Shows the kanban board view for the given board id.
     *
     * <p>
     * If the board (or its workspace) cannot be loaded, the user is
     * sent back to the dashboard instead of crashing.
     * </p>
     */
    public void showBoard(long boardId) {
        Board board;
        try {
            board = boardService.loadBoard(boardId);
        } catch (AppException e) {
            logger.error("Failed to load board (id={})", boardId, e);
            showDashboard();
            return;
        }
        Workspace workspace = workspaceService.findById(board.getWorkspaceId()).orElse(null);
        if (workspace == null) {
            logger.error("Workspace (id={}) for board (id={}) not found", board.getWorkspaceId(), boardId);
            showDashboard();
            return;
        }
        FXMLLoader loader = new FXMLLoader(resolveResource(AppConfig.BOARD_FXML));
        loader.setController(new BoardController(board, workspace, this, boardService, cardService,
                columnService, attachmentService, dueDateService, markdownService, hostServices));
        Parent root = loadRoot(loader, AppConfig.BOARD_FXML);
        showScene(root, AppConfig.BOARD_WINDOW_WIDTH, AppConfig.BOARD_WINDOW_HEIGHT,
                AppConfig.BOARD_CSS, AppConfig.CARD_CSS);
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

    private void showScene(Parent root, String... viewCssFiles) {
        showScene(root, AppConfig.DEFAULT_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_HEIGHT, viewCssFiles);
    }

    private void showScene(Parent root, double width, double height, String... viewCssFiles) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(resolveResource(AppConfig.APP_CSS).toExternalForm());
        for (String cssFile : viewCssFiles) {
            scene.getStylesheets().add(resolveResource(cssFile).toExternalForm());
        }
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
