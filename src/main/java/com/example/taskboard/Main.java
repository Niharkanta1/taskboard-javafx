package com.example.taskboard;

import com.example.taskboard.config.AppConfig;
import com.example.taskboard.config.AppPaths;
import com.example.taskboard.database.DatabaseManager;
import com.example.taskboard.repository.BoardRepository;
import com.example.taskboard.repository.CardRepository;
import com.example.taskboard.repository.CardAttachmentRepository;
import com.example.taskboard.repository.UserRepository;
import com.example.taskboard.repository.WorkspaceRepository;
import com.example.taskboard.repository.impl.BoardRepositoryImpl;
import com.example.taskboard.repository.impl.CardRepositoryImpl;
import com.example.taskboard.repository.impl.CardAttachmentRepositoryImpl;
import com.example.taskboard.repository.impl.UserRepositoryImpl;
import com.example.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.example.taskboard.service.AuthService;
import com.example.taskboard.service.AttachmentService;
import com.example.taskboard.service.BoardService;
import com.example.taskboard.service.CardService;
import com.example.taskboard.service.DevUserBootstrap;
import com.example.taskboard.service.DueDateService;
import com.example.taskboard.service.MarkdownService;
import com.example.taskboard.service.NavigationService;
import com.example.taskboard.service.WorkspaceService;
import com.example.taskboard.service.StorageConfigService;
import com.example.taskboard.session.SessionManager;
import com.example.taskboard.util.GlobalErrorHandler;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX application entry point.
 *
 * <p>
 * Initializes the database, wires the service layer, ensures the
 * development-only initial user exists and opens the login screen.
 * All visual styling lives in CSS.
 * </p>
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private DatabaseManager databaseManager;

    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Starting {} v{}", AppConfig.APP_NAME, AppConfig.APP_VERSION);
        stage.getIcons().add(new Image(getClass().getResourceAsStream(AppConfig.APP_LOGO)));

        StorageConfigService storageConfigService = new StorageConfigService();
        storageConfigService.load();
        AppPaths.configureDataDirectory(storageConfigService.getDataDirectory());
        databaseManager = new DatabaseManager();
        try {
            databaseManager.initialize();
        } catch (RuntimeException e) {
            logger.error("Database initialization failed; application cannot start", e);
            throw e;
        }

        UserRepository userRepository = new UserRepositoryImpl(databaseManager);
        WorkspaceRepository workspaceRepository = new WorkspaceRepositoryImpl(databaseManager);
        BoardRepository boardRepository = new BoardRepositoryImpl(databaseManager);
        CardRepository cardRepository = new CardRepositoryImpl(databaseManager);
        CardAttachmentRepository attachmentRepository = new CardAttachmentRepositoryImpl(databaseManager);
        AuthService authService = new AuthService(userRepository);
        WorkspaceService workspaceService = new WorkspaceService(workspaceRepository);
        BoardService boardService = new BoardService(boardRepository, cardRepository, workspaceRepository);
        CardService cardService = new CardService(cardRepository, boardRepository);
        AttachmentService attachmentService = new AttachmentService(AppPaths.getAttachmentsPath(),
                attachmentRepository);
        DueDateService dueDateService = new DueDateService();
        MarkdownService markdownService = new MarkdownService();
        HostServices hostServices = getHostServices();
        SessionManager sessionManager = new SessionManager();
        NavigationService navigationService = new NavigationService(stage, authService, sessionManager,
                workspaceService, boardService, cardService, attachmentService, dueDateService,
                markdownService, hostServices, storageConfigService);

        if (Boolean.getBoolean("taskboard.release")) {
            logger.info("Release mode enabled; development user bootstrap is disabled.");
        } else {
            new DevUserBootstrap(userRepository, authService).ensureInitialUser();
        }

        navigationService.showStart();

        logger.info("Application started successfully.");
    }

    @Override
    public void stop() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        logger.info("Application stopped.");
    }

    public static void main(String[] args) {
        GlobalErrorHandler.install();
        launch(args);
    }
}
