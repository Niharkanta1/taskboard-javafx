package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.repository.BoardColumnRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link BoardColumnRepository}.
 */
public class BoardColumnRepositoryImpl implements BoardColumnRepository {

    private static final String COLUMNS = "id, board_id, name, position, is_final, created_at, updated_at";

    private final DatabaseManager databaseManager;

    public BoardColumnRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public BoardColumn insert(BoardColumn column) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO board_columns (board_id, name, position, is_final, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, column.getBoardId());
                ps.setString(2, column.getName());
                ps.setDouble(3, column.getPosition());
                ps.setInt(4, column.isFinal() ? 1 : 0);
                ps.setString(5, column.getCreatedAt().toString());
                ps.setString(6, column.getUpdatedAt().toString());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Column insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        column.setId(keys.getLong(1));
                    } else {
                        throw new DatabaseException("Column insert did not return a generated key");
                    }
                }
                return column;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert column", e);
            }
        });
    }

    @Override
    public Optional<BoardColumn> findById(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM board_columns WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapColumn(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find column", e);
            }
        });
    }

    @Override
    public List<BoardColumn> findByBoard(long boardId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM board_columns WHERE board_id = ? ORDER BY position ASC, id ASC")) {
                ps.setLong(1, boardId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<BoardColumn> columns = new ArrayList<>();
                    while (rs.next()) {
                        columns.add(mapColumn(rs));
                    }
                    return columns;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find columns for board", e);
            }
        });
    }

    @Override
    public BoardColumn update(BoardColumn column) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE board_columns SET name = ?, position = ?, is_final = ?, updated_at = ? WHERE id = ?")) {
                ps.setString(1, column.getName());
                ps.setDouble(2, column.getPosition());
                ps.setInt(3, column.isFinal() ? 1 : 0);
                ps.setString(4, column.getUpdatedAt().toString());
                ps.setLong(5, column.getId());
                int updated = ps.executeUpdate();
                if (updated != 1) {
                    throw new DatabaseException("Column update did not affect exactly one row: " + column.getId());
                }
                return column;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update column", e);
            }
        });
    }

    @Override
    public boolean delete(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM board_columns WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete column", e);
            }
        });
    }

    @Override
    public long countByBoard(long boardId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM board_columns WHERE board_id = ?")) {
                ps.setLong(1, boardId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count columns for board", e);
            }
        });
    }

    private BoardColumn mapColumn(ResultSet rs) throws SQLException {
        return new BoardColumn(
                rs.getLong(1),
                rs.getLong(2),
                rs.getString(3),
                rs.getDouble(4),
                rs.getInt(5) == 1,
                parseInstant(rs.getString(6)),
                parseInstant(rs.getString(7)));
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            String normalized = s.trim().replace(' ', 'T');
            if (!normalized.endsWith("Z") && !normalized.contains("+") && normalized.indexOf('-', 10) < 0) {
                normalized += "Z";
            }
            try {
                return Instant.parse(normalized);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}
