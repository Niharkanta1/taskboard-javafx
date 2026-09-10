package com.example.taskboard.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Well-known filesystem locations used by the application.
 *
 * <p>
 * All paths are relative to the application working directory so the
 * application can be started from any location.
 * </p>
 */
public final class AppPaths {

    public static final String DATA_DIR = "data";
    public static final String DATABASE_FILE = "taskboard.db";
    public static final String LOGS_DIR = "logs";
    public static final String ATTACHMENTS_DIR = "attachments";

    private static Path dataDirectory = Paths.get(DATA_DIR).toAbsolutePath().normalize();

    private AppPaths() {
    }

    public static synchronized void configureDataDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Data directory cannot be null");
        }
        dataDirectory = directory.toAbsolutePath().normalize();
    }

    public static synchronized Path getDataDirectory() {
        return dataDirectory;
    }

    public static synchronized Path getDatabasePath() {
        return dataDirectory.resolve(DATABASE_FILE);
    }

    public static synchronized Path getAttachmentsPath() {
        return dataDirectory.resolve(ATTACHMENTS_DIR);
    }
}
