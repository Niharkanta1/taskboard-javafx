package com.example.taskboard.service;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.exception.DatabaseException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.repository.BoardRepository;
import com.example.taskboard.repository.CardRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Card business rules: create, update and delete cards on a board.
 *
 * <p>Timestamp rules:
 * <ul>
 *   <li>entering {@code COMPLETED} sets {@code completed_at} to now;</li>
 *   <li>leaving {@code COMPLETED} (to any other status, including {@code CLOSED})
 *       clears {@code completed_at}.</li>
 * </ul>
 *
 * <p>New cards are appended after the last card of the board
 * ({@code max(position) + 1}).</p>
 */
public class CardService {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int DESCRIPTION_MAX_LENGTH = 5000;

    private final CardRepository cardRepository;
    private final BoardRepository boardRepository;

    public CardService(CardRepository cardRepository, BoardRepository boardRepository) {
        this.cardRepository = cardRepository;
        this.boardRepository = boardRepository;
    }

    public Card createCard(long boardId, String title, String description,
                          CardStatus status, LocalDate dueDate) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ValidationException("Board not found."));

        Instant now = Instant.now();
        Card card = new Card(boardId, normalizeTitle(title), normalizeDescription(description),
                status, nextPosition(boardId), dueDate);
        card.setCreatedAt(now);
        card.setUpdatedAt(now);
        if (status == CardStatus.COMPLETED) {
            card.setCompletedAt(now);
        }
        return insertSafely(card);
    }

    public Card updateCard(long id, String title, String description,
                          CardStatus status, LocalDate dueDate) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Card not found."));

        Instant now = Instant.now();
        card.setTitle(normalizeTitle(title));
        card.setDescription(normalizeDescription(description));
        card.setStatus(status);
        card.setDueDate(dueDate);
        card.setUpdatedAt(now);

        if (status == CardStatus.COMPLETED && card.getCompletedAt() == null) {
            card.setCompletedAt(now);
        } else if (status != CardStatus.COMPLETED) {
            card.setCompletedAt(null);
        }
        return updateSafely(card);
    }

    /**
     * @return {@code true} if the card was deleted, {@code false} if it did not exist.
     */
    public boolean deleteCard(long id) {
        return cardRepository.delete(id) == 1;
    }

    private Card insertSafely(Card card) {
        try {
            return cardRepository.insert(card);
        } catch (DatabaseException e) {
            throw new AppException("Failed to create card", e);
        }
    }

    private Card updateSafely(Card card) {
        try {
            return cardRepository.update(card);
        } catch (DatabaseException e) {
            throw new AppException("Failed to update card", e);
        }
    }

    private double nextPosition(long boardId) {
        List<Card> cards = cardRepository.findByBoard(boardId);
        double max = 0.0;
        for (Card card : cards) {
            max = Math.max(max, card.getPosition());
        }
        return max + 1.0;
    }

    private String normalizeTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Card title is required.");
        }
        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException(
                    "Card title must be at most " + TITLE_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }

    private String normalizeDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ValidationException(
                    "Card description must be at most " + DESCRIPTION_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }
}
