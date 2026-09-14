package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardPriority;
import com.boardly.taskboard.model.CardSeverity;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.ChecklistRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link CardRepository}.
 */
public class CardRepositoryImpl implements CardRepository {

    private static final String COLUMNS = "id, board_id, board_column_id, title, description, position, due_date, created_at, updated_at, completed_at, priority, severity";

    private final DatabaseManager databaseManager;
    private final ChecklistRepository checklistRepository;

    public CardRepositoryImpl(DatabaseManager databaseManager) {
        this(databaseManager, new ChecklistRepositoryImpl(databaseManager));
    }

    public CardRepositoryImpl(DatabaseManager databaseManager, ChecklistRepository checklistRepository) {
        this.databaseManager = databaseManager;
        this.checklistRepository = checklistRepository;
    }

    @Override
    public Card insert(Card card) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO cards (board_id, board_column_id, title, description, position, due_date, created_at, updated_at, completed_at, priority, severity) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, card.getBoardId());
                ps.setLong(2, card.getBoardColumnId());
                ps.setString(3, card.getTitle());
                ps.setString(4, card.getDescription());
                ps.setDouble(5, card.getPosition());
                ps.setString(6, card.getDueDate() == null ? null : card.getDueDate().toString());
                ps.setString(7, card.getCreatedAt().toString());
                ps.setString(8, card.getUpdatedAt().toString());
                ps.setString(9, card.getCompletedAt() == null ? null : card.getCompletedAt().toString());
                ps.setString(10, card.getPriority() != null ? card.getPriority().name() : CardPriority.MEDIUM.name());
                ps.setString(11, card.getSeverity() != null ? card.getSeverity().name() : CardSeverity.MINOR.name());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Card insert did not affect exactly one row");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        card.setId(keys.getLong(1));
                    } else {
                        throw new DatabaseException("Card insert did not return a generated key");
                    }
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
                    "SELECT " + COLUMNS + " FROM cards WHERE board_id = ? ORDER BY position ASC, id ASC")) {
                ps.setLong(1, boardId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Card> cards = new ArrayList<>();
                    while (rs.next()) {
                        cards.add(mapCard(rs));
                    }
                    return cards;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to find cards for board", e);
            }
        });
    }

    @Override
    public Card update(Card card) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE cards SET title = ?, description = ?, board_column_id = ?, position = ?, "
                            + "due_date = ?, updated_at = ?, completed_at = ?, priority = ?, severity = ? WHERE id = ?")) {
                ps.setString(1, card.getTitle());
                ps.setString(2, card.getDescription());
                ps.setLong(3, card.getBoardColumnId());
                ps.setDouble(4, card.getPosition());
                ps.setString(5, card.getDueDate() == null ? null : card.getDueDate().toString());
                ps.setString(6, card.getUpdatedAt().toString());
                ps.setString(7, card.getCompletedAt() == null ? null : card.getCompletedAt().toString());
                ps.setString(8, card.getPriority() != null ? card.getPriority().name() : CardPriority.MEDIUM.name());
                ps.setString(9, card.getSeverity() != null ? card.getSeverity().name() : CardSeverity.MINOR.name());
                ps.setLong(10, card.getId());
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new DatabaseException("Card update did not affect exactly one row: " + card.getId());
                }
                return card;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update card", e);
            }
        });
    }

    @Override
    public boolean delete(long id) {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM cards WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete card", e);
            }
        });
    }

    @Override
    public long count() {
        return databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM cards")) {
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to count cards", e);
            }
        });
    }

    @Override
    public void reorder(long boardId, List<CardPlacement> placements) {
        databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE cards SET board_column_id = ?, position = ?, updated_at = ?, completed_at = ? "
                            + "WHERE id = ? AND board_id = ?")) {
                for (CardPlacement placement : placements) {
                    ps.setLong(1, placement.boardColumnId());
                    ps.setDouble(2, placement.position());
                    ps.setString(3, placement.updatedAt().toString());
                    ps.setString(4, placement.completedAt() == null ? null : placement.completedAt().toString());
                    ps.setLong(5, placement.cardId());
                    ps.setLong(6, boardId);
                    if (ps.executeUpdate() != 1) {
                        throw new DatabaseException(
                                "Card reorder update did not affect exactly one row: " + placement.cardId());
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to reorder cards", e);
            }
        });
    }

    @Override
    public void reorder(CardPlacement placement) {
        databaseManager.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE cards SET board_column_id = ?, position = ?, updated_at = ?, completed_at = ? "
                            + "WHERE id = ?")) {
                ps.setLong(1, placement.boardColumnId());
                ps.setDouble(2, placement.position());
                ps.setString(3, placement.updatedAt().toString());
                ps.setString(4, placement.completedAt() == null ? null : placement.completedAt().toString());
                ps.setLong(5, placement.cardId());
                if (ps.executeUpdate() != 1) {
                    throw new DatabaseException(
                            "Card reorder update did not affect exactly one row: " + placement.cardId());
                }
                return null;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to reorder card", e);
            }
        });
    }

    private Card mapCard(ResultSet rs) throws SQLException {
        Card card = new Card();
        card.setId(rs.getLong("id"));
        card.setBoardId(rs.getLong("board_id"));
        card.setBoardColumnId(rs.getLong("board_column_id"));
        card.setTitle(rs.getString("title"));
        card.setDescription(rs.getString("description"));
        card.setPosition(rs.getDouble("position"));
        String dueDate = rs.getString("due_date");
        card.setDueDate(dueDate == null ? null : LocalDate.parse(dueDate));
        card.setCreatedAt(parseInstant(rs.getString("created_at")));
        card.setUpdatedAt(parseInstant(rs.getString("updated_at")));
        String completedAt = rs.getString("completed_at");
        card.setCompletedAt(completedAt == null ? null : parseInstant(completedAt));
        try {
            card.setPriority(CardPriority.fromCode(rs.getString("priority")));
        } catch (Exception ignored) {
            card.setPriority(CardPriority.MEDIUM);
        }
        try {
            card.setSeverity(CardSeverity.fromCode(rs.getString("severity")));
        } catch (Exception ignored) {
            card.setSeverity(CardSeverity.MINOR);
        }
        card.setChecklists(checklistRepository.findByCardId(card.getId()));
        return card;
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
