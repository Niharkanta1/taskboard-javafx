package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.CardAttachment;
import com.boardly.taskboard.repository.CardAttachmentRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardAttachmentRepositoryImpl implements CardAttachmentRepository {

    private static final String COLUMNS = "id, card_id, file_name, file_path, mime_type, created_at";

    private final DatabaseManager databaseManager;

    public CardAttachmentRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public CardAttachment insert(CardAttachment attachment) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO card_attachments (card_id, file_name, file_path, mime_type, created_at) VALUES (?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, attachment.getCardId());
                ps.setString(2, attachment.getFileName());
                ps.setString(3, attachment.getFilePath());
                ps.setString(4, attachment.getMimeType());
                ps.setString(5, attachment.getCreatedAt().toString());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Card attachment insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    attachment.setId(keys.getLong(1));
                }
                return attachment;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert card attachment", e);
            }
        });
    }

    @Override
    public Optional<CardAttachment> findById(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM card_attachments WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapAttachment(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find card attachment", e);
            }
        });
    }

    @Override
    public List<CardAttachment> findByCard(long cardId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM card_attachments WHERE card_id = ? ORDER BY id")) {
                ps.setLong(1, cardId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<CardAttachment> attachments = new ArrayList<>();
                    while (rs.next()) {
                        attachments.add(mapAttachment(rs));
                    }
                    return attachments;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to list attachments for card", e);
            }
        });
    }

    @Override
    public long count() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM card_attachments")) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count card attachments", e);
            }
        });
    }

    private CardAttachment mapAttachment(ResultSet rs) throws SQLException {
        CardAttachment attachment = new CardAttachment();
        attachment.setId(rs.getLong("id"));
        attachment.setCardId(rs.getLong("card_id"));
        attachment.setFileName(rs.getString("file_name"));
        attachment.setFilePath(rs.getString("file_path"));
        attachment.setMimeType(rs.getString("mime_type"));
        attachment.setCreatedAt(Instant.parse(rs.getString("created_at")));
        return attachment;
    }
}
