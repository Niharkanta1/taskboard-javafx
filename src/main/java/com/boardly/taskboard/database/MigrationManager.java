package com.boardly.taskboard.database;

import com.boardly.taskboard.exception.DatabaseException;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies Flyway database migrations from {@code classpath:db/migration}.
 *
 * <p>Safe to call on every application start: already-applied migrations
 * are recorded in the schema history table and skipped.</p>
 */
public final class MigrationManager {

    private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);

    private final Flyway flyway;

    public MigrationManager(String jdbcUrl) {
        this.flyway = Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .table("flyway_schema_history")
                .load();
    }

    public void migrate() {
        try {
            int applied = flyway.migrate().migrationsExecuted;
            logger.info("Database migrations applied: {} change(s)", applied);
        } catch (FlywayException e) {
            logger.error("Database migration failed", e);
            throw new DatabaseException("Database migration failed", e);
        }
    }
}
