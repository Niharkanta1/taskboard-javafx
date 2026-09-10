package com.example.taskboard.service;

import com.example.taskboard.config.AppPaths;
import com.example.taskboard.exception.AppException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists the user-selected data directory outside the data directory itself.
 */
public class StorageConfigService {

    private static final String DATA_DIRECTORY_KEY = "dataDirectory";
    private final Path settingsFile;
    private Path dataDirectory;

    public StorageConfigService() {
        this(Path.of(System.getProperty("user.home"), ".taskboard", "settings.properties"));
    }

    public StorageConfigService(Path settingsFile) {
        this.settingsFile = settingsFile.toAbsolutePath().normalize();
        this.dataDirectory = Path.of(AppPaths.DATA_DIR).toAbsolutePath().normalize();
    }

    public void load() {
        if (!Files.isRegularFile(settingsFile)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
            String configured = properties.getProperty(DATA_DIRECTORY_KEY);
            if (configured != null && !configured.isBlank()) {
                dataDirectory = Path.of(configured).toAbsolutePath().normalize();
            }
        } catch (IOException | InvalidPathException e) {
            throw new AppException("Unable to load storage settings.", e);
        }
    }

    public void save(Path directory) {
        if (directory == null) {
            throw new AppException("A data directory is required.");
        }
        Path normalized = directory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            Files.createDirectories(settingsFile.getParent());
            Properties properties = new Properties();
            properties.setProperty(DATA_DIRECTORY_KEY, normalized.toString());
            try (OutputStream output = Files.newOutputStream(settingsFile)) {
                properties.store(output, "TaskBoard storage settings");
            }
            dataDirectory = normalized;
        } catch (IOException e) {
            throw new AppException("Unable to save storage settings.", e);
        }
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Path getSettingsFile() {
        return settingsFile;
    }
}