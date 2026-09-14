package com.boardly.taskboard.database;

import com.boardly.taskboard.exception.DatabaseException;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies Flyway database migrations from {@code classpath:db/migration}.
 *
 * <p>
 * Safe to call on every application start: already-applied migrations
 * are recorded in the schema history table and skipped.
 * </p>
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
            // A checksum mismatch means the migration files changed after this
            // database was created. Auto-repair would rewrite the history and
            // leave the schema out of sync with the code, so fail loudly instead.
            if (isChecksumMismatch(e)) {
                logger.error("Migration files no longer match this database; refusing to auto-repair. "
                        + "Restore the original migration files or reset the database.", e);
                throw new DatabaseException("Database migration validation failed", e);
            }
            logger.warn("Initial migration/validation encountered an issue; attempting schema repair: {}",
                    e.getMessage());
            try {
                flyway.repair();
                int applied = flyway.migrate().migrationsExecuted;
                logger.info("Database migrations applied after repair: {} change(s)", applied);
            } catch (FlywayException repairError) {
                logger.error("Database migration failed after repair attempt", repairError);
                throw new DatabaseException("Database migration failed", repairError);
            }
        }
    }

    private static boolean isChecksumMismatch(FlywayException e) {
        return e.getMessage() != null && e.getMessage().contains("checksum mismatch");
    }
}
