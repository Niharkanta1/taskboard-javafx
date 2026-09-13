package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.repository.TagRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TagRepositoryImpl implements TagRepository {

    private static final String COLUMNS = "id, name, color, created_at, updated_at";

    private final DatabaseManager databaseManager;

    public TagRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Tag insert(Tag tag) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO tags (name, color, created_at, updated_at) VALUES (?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, tag.getName());
                ps.setString(2, tag.getColor());
                ps.setString(3, tag.getCreatedAt().toString());
                ps.setString(4, tag.getUpdatedAt().toString());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Tag insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        tag.setId(keys.getLong(1));
                    } else {
                        throw new DatabaseException("Tag insert did not return a generated key");
                    }
                }
                return tag;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert tag", e);
            }
        });
    }

    @Override
    public Optional<Tag> findById(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM tags WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapTag(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find tag by id", e);
            }
        });
    }

    @Override
    public Optional<Tag> findByName(String name) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM tags WHERE LOWER(name) = LOWER(?)")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapTag(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find tag by name", e);
            }
        });
    }

    @Override
    public List<Tag> findAll() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM tags ORDER BY name ASC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<Tag> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapTag(rs));
                    }
                    return list;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to list tags", e);
            }
        });
    }

    @Override
    public Tag update(Tag tag) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE tags SET name = ?, color = ?, updated_at = ? WHERE id = ?")) {
                ps.setString(1, tag.getName());
                ps.setString(2, tag.getColor());
                ps.setString(3, tag.getUpdatedAt().toString());
                ps.setLong(4, tag.getId());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Tag update did not affect exactly one row: " + tag.getId());
                }
                return tag;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update tag", e);
            }
        });
    }

    @Override
    public boolean delete(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM tags WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete tag", e);
            }
        });
    }

    @Override
    public long count() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM tags")) {
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count tags", e);
            }
        });
    }

    @Override
    public List<Tag> findByCardId(long cardId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT t.id, t.name, t.color, t.created_at, t.updated_at "
                            + "FROM tags t JOIN card_tags ct ON t.id = ct.tag_id "
                            + "WHERE ct.card_id = ? ORDER BY t.name ASC")) {
                ps.setLong(1, cardId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Tag> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapTag(rs));
                    }
                    return list;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find tags for card: " + cardId, e);
            }
        });
    }

    @Override
    public Map<Long, List<Tag>> findTagsByBoard(long boardId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT ct.card_id, t.id, t.name, t.color, t.created_at, t.updated_at "
                            + "FROM card_tags ct "
                            + "JOIN tags t ON ct.tag_id = t.id "
                            + "JOIN cards c ON ct.card_id = c.id "
                            + "WHERE c.board_id = ? ORDER BY t.name ASC")) {
                ps.setLong(1, boardId);
                try (ResultSet rs = ps.executeQuery()) {
                    Map<Long, List<Tag>> result = new HashMap<>();
                    while (rs.next()) {
                        long cardId = rs.getLong("card_id");
                        Tag tag = new Tag(
                                rs.getLong("id"),
                                rs.getString("name"),
                                rs.getString("color"),
                                parseInstant(rs.getString("created_at")),
                                parseInstant(rs.getString("updated_at")));
                        result.computeIfAbsent(cardId, k -> new ArrayList<>()).add(tag);
                    }
                    return result;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find tags for board: " + boardId, e);
            }
        });
    }

    @Override
    public void setCardTags(long cardId, List<Long> tagIds) {
        databaseManager.inTransaction(connection -> {
            try {
                try (PreparedStatement deletePs = connection.prepareStatement(
                        "DELETE FROM card_tags WHERE card_id = ?")) {
                    deletePs.setLong(1, cardId);
                    deletePs.executeUpdate();
                }
                if (tagIds != null && !tagIds.isEmpty()) {
                    try (PreparedStatement insertPs = connection.prepareStatement(
                            "INSERT OR IGNORE INTO card_tags (card_id, tag_id) VALUES (?, ?)")) {
                        for (Long tagId : tagIds) {
                            if (tagId != null && tagId > 0) {
                                insertPs.setLong(1, cardId);
                                insertPs.setLong(2, tagId);
                                insertPs.addBatch();
                            }
                        }
                        insertPs.executeBatch();
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update tags for card: " + cardId, e);
            }
        });
    }

    private Tag mapTag(ResultSet rs) throws SQLException {
        return new Tag(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("color"),
                parseInstant(rs.getString("created_at")),
                parseInstant(rs.getString("updated_at")));
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
