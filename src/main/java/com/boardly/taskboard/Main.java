package com.boardly.taskboard;

import com.boardly.taskboard.config.AppConfig;
import com.boardly.taskboard.config.AppPaths;
import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.CardAttachmentRepository;
import com.boardly.taskboard.repository.TagRepository;
import com.boardly.taskboard.repository.UserRepository;
import com.boardly.taskboard.repository.WorkspaceRepository;
import com.boardly.taskboard.repository.impl.BoardColumnRepositoryImpl;
import com.boardly.taskboard.repository.impl.BoardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardRepositoryImpl;
import com.boardly.taskboard.repository.impl.CardAttachmentRepositoryImpl;
import com.boardly.taskboard.repository.impl.TagRepositoryImpl;
import com.boardly.taskboard.repository.impl.UserRepositoryImpl;
import com.boardly.taskboard.repository.impl.WorkspaceRepositoryImpl;
import com.boardly.taskboard.service.AuthService;
import com.boardly.taskboard.service.AttachmentService;
import com.boardly.taskboard.service.BoardService;
import com.boardly.taskboard.service.CardService;
import com.boardly.taskboard.service.ColumnService;
import com.boardly.taskboard.service.DevUserBootstrap;
import com.boardly.taskboard.service.DueDateService;
import com.boardly.taskboard.service.MarkdownService;
import com.boardly.taskboard.service.NavigationService;
import com.boardly.taskboard.service.TagService;
import com.boardly.taskboard.service.WorkspaceService;
import com.boardly.taskboard.service.StorageConfigService;
import com.boardly.taskboard.session.SessionManager;
import com.boardly.taskboard.util.GlobalErrorHandler;

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
        BoardColumnRepository columnRepository = new BoardColumnRepositoryImpl(databaseManager);
        TagRepository tagRepository = new TagRepositoryImpl(databaseManager);
        CardRepository cardRepository = new CardRepositoryImpl(databaseManager);
        CardAttachmentRepository attachmentRepository = new CardAttachmentRepositoryImpl(databaseManager);
        AuthService authService = new AuthService(userRepository);
        WorkspaceService workspaceService = new WorkspaceService(workspaceRepository);
        BoardService boardService = new BoardService(boardRepository, cardRepository, columnRepository,
                workspaceRepository, tagRepository);
        CardService cardService = new CardService(cardRepository, boardRepository, columnRepository, tagRepository);
        TagService tagService = new TagService(tagRepository);
        ColumnService columnService = new ColumnService(columnRepository, cardRepository, cardService);
        AttachmentService attachmentService = new AttachmentService(AppPaths.getAttachmentsPath(),
                attachmentRepository);
        DueDateService dueDateService = new DueDateService();
        MarkdownService markdownService = new MarkdownService();
        HostServices hostServices = getHostServices();
        SessionManager sessionManager = new SessionManager();
        NavigationService navigationService = new NavigationService(stage, authService, sessionManager,
                workspaceService, boardService, cardService, columnService, tagService, attachmentService,
                dueDateService,
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
