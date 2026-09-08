package com.example.taskboard.repository;

import com.example.taskboard.model.Card;

import java.util.List;
import java.util.Optional;

/**
 * Data access for cards.
 */
public interface CardRepository {

    Card insert(Card card);

    Optional<Card> findById(long id);

    List<Card> findByBoard(long boardId);

    long count();
}
