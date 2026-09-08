package com.example.taskboard.repository.impl;

import com.example.taskboard.database.DatabaseManager;
import com.example.taskboard.exception.DatabaseException;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.repository.CardRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardRepositoryImpl implements CardRepository {

    private static final String COLUMNS =
            "id, board_id, title, description, status, position, due_date, created_at, updated_at, completed_at";

    private final DatabaseManager databaseManager;

    public CardRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Card insert(Card card) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO cards (board_id, title, description, status, position, due_date, created_at, updated_at, completed_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, card.getBoardId());
                ps.setString(2, card.getTitle());
                ps.setString(3, card.getDescription());
                ps.setString(4, card.getStatus().getCode());
                ps.setDouble(5, card.getPosition());
                ps.setString(6, card.getDueDate() == null ? null : card.getDueDate().toString());
                ps.setString(7, card.getCreatedAt().toString());
                ps.setString(8, card.getUpdatedAt().toString());
                ps.setString(9, card.getCompletedAt() == null ? null : card.getCompletedAt().toString());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Card insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    card.setId(keys.getLong(1));
                }
                return card;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert card", e);
            }
        });
    }

    @Override
    public Optional<Card> findById(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM cards WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapCard(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find card", e);
            }
        });
    }

    @Override
    public List<Card> findByBoard(long boardId) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM cards WHERE board_id = ? ORDER BY position, id")) {
                ps.setLong(1, boardId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Card> cards = new ArrayList<>();
                    while (rs.next()) {
                        cards.add(mapCard(rs));
                    }
                    return cards;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to list cards for board", e);
            }
        });
    }

    @Override
    public long count() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM cards")) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count cards", e);
            }
        });
    }

    private Card mapCard(ResultSet rs) throws SQLException {
        Card card = new Card();
        card.setId(rs.getLong("id"));
        card.setBoardId(rs.getLong("board_id"));
        card.setTitle(rs.getString("title"));
        card.setDescription(rs.getString("description"));
        card.setStatus(CardStatus.fromCode(rs.getString("status")));
        card.setPosition(rs.getDouble("position"));
        String dueDate = rs.getString("due_date");
        card.setDueDate(dueDate == null ? null : LocalDate.parse(dueDate));
        card.setCreatedAt(Instant.parse(rs.getString("created_at")));
        card.setUpdatedAt(Instant.parse(rs.getString("updated_at")));
        String completedAt = rs.getString("completed_at");
        card.setCompletedAt(completedAt == null ? null : Instant.parse(completedAt));
        return card;
    }
}
