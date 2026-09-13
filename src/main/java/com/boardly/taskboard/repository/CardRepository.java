package com.boardly.taskboard.repository;

import com.boardly.taskboard.model.Card;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence interface for cards.
 */
public interface CardRepository {

    Card insert(Card card);

    Optional<Card> findById(long id);

    /**
     * All cards of the given board, ordered by their position.
     * Cards are returned in a single list; the caller groups them by column.
     */
    List<Card> findByBoard(long boardId);

    Card update(Card card);

    boolean delete(long id);

    long count();

    /**
     * Parameters for an atomic card placement operation.
     */
    record CardPlacement(long cardId, long boardColumnId, double position,
            Instant updatedAt, Instant completedAt) {
    }

    /**
     * Reorders multiple cards on a board in a single transaction.
     */
    void reorder(long boardId, List<CardPlacement> placements);

    /**
     * Moves a single card to the given column and position in a single transaction.
     */
    void reorder(CardPlacement placement);
}
