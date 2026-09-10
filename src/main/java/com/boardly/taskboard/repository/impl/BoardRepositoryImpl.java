package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.repository.BoardRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoardRepositoryImpl implements BoardRepository {

    private static final String COLUMNS = "id, workspace_id, name, description, created_at, updated_at";

    private final DatabaseManager databaseManager;

    public BoardRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Board insert(Board board) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO boards (workspace_id, name, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, board.getWorkspaceId());
                ps.setString(2, board.getName());
                ps.setString(3, board.getDescription());
                ps.setString(4, board.getCreatedAt().toString());
                ps.setString(5, board.getUpdatedAt().toString());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Board insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    board.setId(keys.getLong(1));
                }
                return board;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert board", e);
            }
        });
    }

    @Override
    public Optional<Board> findById(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM boards WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapBoard(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find board", e);
            }
        });
    }

    @Override
    public List<Board> findByWorkspace(long workspaceId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM boards WHERE workspace_id = ? ORDER BY id")) {
                ps.setLong(1, workspaceId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Board> boards = new ArrayList<>();
                    while (rs.next()) {
                        boards.add(mapBoard(rs));
                    }
                    return boards;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to list boards for workspace", e);
            }
        });
    }

    @Override
    public Board update(Board board) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE boards SET name = ?, description = ?, updated_at = ? WHERE id = ?")) {
                ps.setString(1, board.getName());
                ps.setString(2, board.getDescription());
                ps.setString(3, board.getUpdatedAt().toString());
                ps.setLong(4, board.getId());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Board update did not affect exactly one row");
                }
                return board;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update board", e);
            }
        });
    }

    @Override
    public int delete(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM boards WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete board", e);
            }
        });
    }

    @Override
    public long count() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM boards")) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count boards", e);
            }
        });
    }

    private Board mapBoard(ResultSet rs) throws SQLException {
        Board board = new Board();
        board.setId(rs.getLong("id"));
        board.setWorkspaceId(rs.getLong("workspace_id"));
        board.setName(rs.getString("name"));
        board.setDescription(rs.getString("description"));
        board.setCreatedAt(Instant.parse(rs.getString("created_at")));
        board.setUpdatedAt(Instant.parse(rs.getString("updated_at")));
        return board;
    }
}
