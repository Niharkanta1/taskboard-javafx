package com.boardly.taskboard.database;

import com.boardly.taskboard.config.AppPaths;
import com.boardly.taskboard.exception.DatabaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

/**
 * Owns the SQLite connection for the application.
 *
 * <p>
 * Creates the data directory and database file on first start, enables
 * foreign-key enforcement on the connection and runs Flyway migrations.
 * Multi-statement writes must go through {@link #inTransaction} so they
 * succeed or fail together.
 * </p>
 */
public final class DatabaseManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final Path databasePath;
    private Connection connection;

    public DatabaseManager() {
        this(AppPaths.getDatabasePath());
    }

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
    }

    public void initialize() {
        if (connection != null) {
            logger.warn("DatabaseManager.initialize() called more than once; ignoring.");
            return;
        }
        try {
            Path absolute = databasePath.toAbsolutePath();
            Path parent = absolute.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String jdbcUrl = "jdbc:sqlite:" + absolute;
            connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
            }
            new MigrationManager(jdbcUrl).migrate();
            logger.info("Database ready at {}", absolute);
        } catch (IOException e) {
            throw new DatabaseException("Failed to create data directory for " + databasePath, e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database at " + databasePath, e);
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database is not initialized");
        }
        return connection;
    }

    /**
     * Runs the given work inside a single transaction: commits on success,
     * rolls back on failure.
     */
    public <T> T inTransaction(Function<Connection, T> work) {
        if (connection == null) {
            throw new IllegalStateException("Database is not initialized");
        }
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to begin transaction", e);
        }
        try {
            T result = work.apply(connection);
            try {
                connection.commit();
            } catch (SQLException e) {
                rollbackQuietly();
                throw new DatabaseException("Transaction commit failed", e);
            }
            return result;
        } catch (RuntimeException e) {
            rollbackQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            throw new DatabaseException("Transaction failed", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new DatabaseException("Failed to restore autocommit mode", e);
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            logger.warn("Rollback failed", e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Failed to close database connection", e);
            }
            connection = null;
            logger.info("Database connection closed");
        }
    }
}
