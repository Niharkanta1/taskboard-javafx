package com.example.taskboard.repository;

import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

/**
 * Data access for cards.
 */
public interface CardRepository {

    record CardPlacement(long cardId, CardStatus status, double position,
            Instant updatedAt, Instant completedAt) {
    }

    Card insert(Card card);

    Optional<Card> findById(long id);

    List<Card> findByBoard(long boardId);

    Card update(Card card);

    default void reorder(long boardId, List<CardPlacement> placements) {
        throw new UnsupportedOperationException("Card reorder is not supported");
    }

    int delete(long id);

    long count();
}
