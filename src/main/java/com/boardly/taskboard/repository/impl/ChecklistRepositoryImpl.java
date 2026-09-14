package com.boardly.taskboard.repository.impl;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Checklist;
import com.boardly.taskboard.model.ChecklistItem;
import com.boardly.taskboard.repository.ChecklistRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite implementation of {@link ChecklistRepository}.
 */
public class ChecklistRepositoryImpl implements ChecklistRepository {

    private static final String CHECKLIST_COLUMNS = "id, card_id, title, position, created_at, updated_at";
    private static final String ITEM_COLUMNS = "id, checklist_id, text, completed, position, created_at, updated_at";

    private final DatabaseManager databaseManager;

    public ChecklistRepositoryImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<Checklist> findByCardId(long cardId) {
        // Read-only; deliberately not wrapped in a transaction so this
        // method can be called from inside another repository's
        // transaction without committing it prematurely.
        try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(
                "SELECT " + CHECKLIST_COLUMNS + " FROM checklists WHERE card_id = ? ORDER BY position ASC, id ASC")) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Checklist> checklists = new ArrayList<>();
                while (rs.next()) {
                    checklists.add(mapChecklist(rs));
                }
                return checklists;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find checklists for card: " + cardId, e);
        }
    }

    @Override
    public void syncForCard(long cardId, List<Checklist> checklists) {
        databaseManager.inTransaction(connection -> {
            try {
                Instant now = Instant.now();
                // Remove all existing checklists of the card; their items are
                // removed by the ON DELETE CASCADE foreign key.
                try (PreparedStatement deletePs = connection.prepareStatement(
                        "DELETE FROM checklists WHERE card_id = ?")) {
                    deletePs.setLong(1, cardId);
                    deletePs.executeUpdate();
                }

                if (checklists == null || checklists.isEmpty()) {
                    return null;
                }

                try (PreparedStatement insertPs = connection.prepareStatement(
                        "INSERT INTO checklists (card_id, title, position, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                    PreparedStatement insertItemPs = connection.prepareStatement(
                            "INSERT INTO checklist_items (checklist_id, text, completed, position, created_at, updated_at) "
                                    + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (int i = 0; i < checklists.size(); i++) {
                        Checklist checklist = checklists.get(i);
                        requireTitle(checklist);
                        long position = i + 1;

                        insertPs.setLong(1, cardId);
                        insertPs.setString(2, checklist.getTitle());
                        insertPs.setLong(3, position);
                        insertPs.setString(4, now.toString());
                        insertPs.setString(5, now.toString());
                        if (insertPs.executeUpdate() != 1) {
                            throw new DatabaseException("Checklist insert did not affect exactly one row");
                        }
                        try (ResultSet keys = insertPs.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new DatabaseException("Checklist insert did not return a generated key");
                            }
                            checklist.setId(keys.getLong(1));
                        }

                        for (int j = 0; j < checklist.getItems().size(); j++) {
                            ChecklistItem item = checklist.getItems().get(j);
                            if (item.getText() == null || item.getText().isBlank()) {
                                continue;
                            }
                            insertItemPs.setLong(1, checklist.getId());
                            insertItemPs.setString(2, item.getText());
                            insertItemPs.setInt(3, item.isCompleted() ? 1 : 0);
                            insertItemPs.setLong(4, j + 1);
                            insertItemPs.setString(5, now.toString());
                            insertItemPs.setString(6, now.toString());
                            insertItemPs.executeUpdate();
                        }
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to sync checklists for card: " + cardId, e);
            }
        });
    }

    private Checklist mapChecklist(ResultSet rs) throws SQLException {
        Checklist checklist = new Checklist();
        checklist.setId(rs.getLong("id"));
        checklist.setTitle(rs.getString("title"));
        checklist.getItems().addAll(findItems(rs.getStatement().getConnection(), checklist.getId()));
        return checklist;
    }

    private List<ChecklistItem> findItems(java.sql.Connection connection, long checklistId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " + ITEM_COLUMNS + " FROM checklist_items WHERE checklist_id = ? ORDER BY position ASC, id ASC")) {
            ps.setLong(1, checklistId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ChecklistItem> items = new ArrayList<>();
                while (rs.next()) {
                    ChecklistItem item = new ChecklistItem();
                    item.setId(rs.getLong("id"));
                    item.setText(rs.getString("text"));
                    item.setCompleted(rs.getInt("completed") == 1);
                    items.add(item);
                }
                return items;
            }
        }
    }

    private void requireTitle(Checklist checklist) {
        if (checklist.getTitle() == null || checklist.getTitle().isBlank()) {
            throw new ValidationException("Checklist title must not be empty.");
        }
    }
}
