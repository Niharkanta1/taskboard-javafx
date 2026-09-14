package com.boardly.taskboard.repository;

import com.boardly.taskboard.model.Checklist;

import java.util.List;

/**
 * Persistence interface for card checklists and their items.
 *
 * <p>
 * Checklists and items are owned by a card; deleting a card cascades to
 * its checklists and items via the database foreign keys.
 * </p>
 */
public interface ChecklistRepository {

    /**
     * All checklists of the given card, ordered by their position.
     * Each checklist contains its items ordered by their position.
     */
    List<Checklist> findByCardId(long cardId);

    /**
     * Replaces the card's checklists with the given list in a single
     * transaction: existing checklists are removed, the given checklists
     * are inserted (or updated when they already have an id).
     */
    void syncForCard(long cardId, List<Checklist> checklists);
}
