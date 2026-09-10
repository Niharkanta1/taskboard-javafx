package com.example.taskboard.service;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.exception.DatabaseException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Card;
import com.example.taskboard.model.CardStatus;
import com.example.taskboard.repository.BoardRepository;
import com.example.taskboard.repository.CardRepository;
import com.example.taskboard.repository.CardRepository.CardPlacement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Map;

/**
 * Card business rules: create, update and delete cards on a board.
 *
 * <p>
 * Timestamp rules:
 * <ul>
 * <li>entering {@code COMPLETED} sets {@code completed_at} to now;</li>
 * <li>leaving {@code COMPLETED} (to any other status, including {@code CLOSED})
 * clears {@code completed_at}.</li>
 * </ul>
 *
 * <p>
 * New cards are appended after the last card of the board
 * ({@code max(position) + 1}).
 * </p>
 */
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

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
     * @return {@code true} if the card was deleted, {@code false} if it did not
     *         exist.
     */
    public boolean deleteCard(long id) {
        boolean deleted = cardRepository.delete(id) == 1;
        if (deleted) {
            logger.info("Deleted card (id={})", id);
        }
        return deleted;
    }

    /** Moves a card to a zero-based index in its target status column. */
    public Card moveCard(long cardId, CardStatus targetStatus, int targetIndex) {
        if (targetStatus == null) {
            throw new ValidationException("Card status is required.");
        }
        Card moving = cardRepository.findById(cardId)
                .orElseThrow(() -> new ValidationException("Card not found."));
        List<Card> boardCards = cardRepository.findByBoard(moving.getBoardId());
        Map<CardStatus, List<Card>> columns = new EnumMap<>(CardStatus.class);
        for (CardStatus status : CardStatus.values()) {
            columns.put(status, new ArrayList<>());
        }
        for (Card card : boardCards) {
            if (card.getId() != cardId) {
                columns.get(card.getStatus()).add(card);
            }
        }

        List<Card> targetColumn = columns.get(targetStatus);
        int insertionIndex = Math.max(0, Math.min(targetIndex, targetColumn.size()));
        targetColumn.add(insertionIndex, moving);

        Instant now = Instant.now();
        Instant completedAt = moving.getCompletedAt();
        if (targetStatus == CardStatus.COMPLETED && completedAt == null) {
            completedAt = now;
        } else if (targetStatus != CardStatus.COMPLETED) {
            completedAt = null;
        }
        moving.setStatus(targetStatus);
        moving.setCompletedAt(completedAt);
        moving.setUpdatedAt(now);

        List<CardPlacement> placements = new ArrayList<>();
        for (CardStatus status : CardStatus.values()) {
            List<Card> column = columns.get(status);
            for (int index = 0; index < column.size(); index++) {
                Card card = column.get(index);
                Instant cardCompletedAt = card.getCompletedAt();
                if (card.getId() == cardId) {
                    cardCompletedAt = completedAt;
                    card.setPosition(index + 1.0);
                    card.setStatus(targetStatus);
                    card.setCompletedAt(completedAt);
                    card.setUpdatedAt(now);
                }
                placements.add(new CardPlacement(card.getId(), status, index + 1.0, now, cardCompletedAt));
            }
        }
        try {
            cardRepository.reorder(moving.getBoardId(), placements);
            moving.setPosition(insertionIndex + 1.0);
            logger.info("Moved card (id={}) to status {} at position {}",
                    cardId, targetStatus, moving.getPosition());
            return moving;
        } catch (DatabaseException e) {
            throw new AppException("Failed to move card", e);
        }
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
