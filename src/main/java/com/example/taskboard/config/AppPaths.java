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

    public static final Path DATABASE_PATH = Paths.get(DATA_DIR, DATABASE_FILE);
    public static final Path ATTACHMENTS_PATH = Paths.get(DATA_DIR, ATTACHMENTS_DIR);

    private AppPaths() {
    }
}
