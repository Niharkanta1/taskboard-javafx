package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.Workspace;
import com.boardly.taskboard.repository.WorkspaceRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private static final String COLUMNS = "id, owner_user_id, name, description, created_at, updated_at";

    private final DatabaseManager databaseManager;

    public WorkspaceRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Workspace insert(Workspace workspace) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO workspaces (owner_user_id, name, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                if (workspace.getOwnerUserId() == 0) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setLong(1, workspace.getOwnerUserId());
                }
                ps.setString(2, workspace.getName());
                ps.setString(3, workspace.getDescription());
                ps.setString(4, workspace.getCreatedAt().toString());
                ps.setString(5, workspace.getUpdatedAt().toString());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Workspace insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    workspace.setId(keys.getLong(1));
                }
                return workspace;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert workspace", e);
            }
        });
    }

    @Override
    public Workspace update(Workspace workspace) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE workspaces SET name = ?, description = ?, updated_at = ? WHERE id = ?")) {
                ps.setString(1, workspace.getName());
                ps.setString(2, workspace.getDescription());
                ps.setString(3, workspace.getUpdatedAt().toString());
                ps.setLong(4, workspace.getId());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Workspace update did not affect exactly one row");
                }
                return workspace;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update workspace", e);
            }
        });
    }

    @Override
    public Optional<Workspace> findById(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM workspaces WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapWorkspace(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find workspace", e);
            }
        });
    }

    @Override
    public List<Workspace> findAll() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM workspaces ORDER BY id")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<Workspace> workspaces = new ArrayList<>();
                    while (rs.next()) {
                        workspaces.add(mapWorkspace(rs));
                    }
                    return workspaces;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to list workspaces", e);
            }
        });
    }

    @Override
    public List<Workspace> findAllByOwner(long ownerUserId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM workspaces WHERE owner_user_id = ? ORDER BY id")) {
                ps.setLong(1, ownerUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Workspace> workspaces = new ArrayList<>();
                    while (rs.next()) {
                        workspaces.add(mapWorkspace(rs));
                    }
                    return workspaces;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to list owned workspaces", e);
            }
        });
    }

    @Override
    public Optional<Workspace> findByIdAndOwner(long id, long ownerUserId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM workspaces WHERE id = ? AND owner_user_id = ?")) {
                ps.setLong(1, id);
                ps.setLong(2, ownerUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapWorkspace(rs)) : Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find owned workspace", e);
            }
        });
    }

    @Override
    public int delete(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM workspaces WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete workspace", e);
            }
        });
    }

    @Override
    public long count() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM workspaces")) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count workspaces", e);
            }
        });
    }

    private Workspace mapWorkspace(ResultSet rs) throws SQLException {
        Workspace workspace = new Workspace();
        workspace.setId(rs.getLong("id"));
        workspace.setOwnerUserId(rs.getLong("owner_user_id"));
        workspace.setName(rs.getString("name"));
        workspace.setDescription(rs.getString("description"));
        workspace.setCreatedAt(Instant.parse(rs.getString("created_at")));
        workspace.setUpdatedAt(Instant.parse(rs.getString("updated_at")));
        return workspace;
    }
}
