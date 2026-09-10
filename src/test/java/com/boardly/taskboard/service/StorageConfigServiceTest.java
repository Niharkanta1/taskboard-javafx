package com.boardly.taskboard.service;

import com.boardly.taskboard.config.AppPaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageConfigServiceTest {

    @Test
    void savesAndLoadsConfiguredDataDirectory() throws Exception {
        Path temp = Files.createTempDirectory("taskboard-settings-test");
        Path settings = temp.resolve("config").resolve("settings.properties");
        Path data = temp.resolve("portable-data");

        StorageConfigService saved = new StorageConfigService(settings);
        saved.save(data);

        StorageConfigService loaded = new StorageConfigService(settings);
        loaded.load();

        assertEquals(data.toAbsolutePath().normalize(), loaded.getDataDirectory());
        assertTrue(Files.isRegularFile(settings));
    }

    @Test
    void appPathsExposeConfiguredDatabaseAndAttachments() throws Exception {
        Path data = Files.createTempDirectory("taskboard-paths-test");
        Path original = AppPaths.getDataDirectory();
        try {
            AppPaths.configureDataDirectory(data);
            assertEquals(data.resolve("taskboard.db"), AppPaths.getDatabasePath());
            assertEquals(data.resolve("attachments"), AppPaths.getAttachmentsPath());
        } finally {
            AppPaths.configureDataDirectory(original);
        }
    }
}
