package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.model.CardPriority;
import com.boardly.taskboard.model.CardSeverity;
import com.boardly.taskboard.model.Checklist;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.ChecklistRepository;
import com.boardly.taskboard.repository.TagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for cards: validation, persistence, and moves.
 *
 * <p>
 * Cards belong to a board column; moving a card changes its column and
 * position. Entering a final column stamps {@code completedAt}; leaving
 * a final column clears it.
 * </p>
 */
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    public static final int TITLE_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 5000;

    private static final double MIN_POSITION = 1.0;
    private static final double POSITION_STEP = 1.0;

    private final CardRepository cardRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final TagRepository tagRepository;
    private final ChecklistRepository checklistRepository;

    public CardService(CardRepository cardRepository, BoardRepository boardRepository,
            BoardColumnRepository columnRepository, TagRepository tagRepository) {
        this(cardRepository, boardRepository, columnRepository, tagRepository, null);
    }

    public CardService(CardRepository cardRepository, BoardRepository boardRepository,
            BoardColumnRepository columnRepository, TagRepository tagRepository,
            ChecklistRepository checklistRepository) {
        this.cardRepository = cardRepository;
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.tagRepository = tagRepository;
        this.checklistRepository = checklistRepository;
    }

    public CardService(CardRepository cardRepository, BoardRepository boardRepository,
            BoardColumnRepository columnRepository) {
        this(cardRepository, boardRepository, columnRepository, null);
    }

    public CardService(CardRepository cardRepository, BoardColumnRepository columnRepository) {
        this(cardRepository, null, columnRepository, null);
    }

    public Card createCard(long boardId, String title, String description,
            long boardColumnId, LocalDate dueDate) {
        return createCard(boardId, title, description, boardColumnId, dueDate, CardPriority.MEDIUM, CardSeverity.MINOR,
                null);
    }

    public Card createCard(long boardId, String title, String description,
            long boardColumnId, LocalDate dueDate, CardPriority priority, CardSeverity severity, List<Long> tagIds) {
        return createCard(boardId, title, description, boardColumnId, dueDate, priority, severity, tagIds, null);
    }

    public Card createCard(long boardId, String title, String description,
            long boardColumnId, LocalDate dueDate, CardPriority priority, CardSeverity severity,
            List<Long> tagIds, List<Checklist> checklists) {
        if (boardId <= 0) {
            throw new ValidationException("Board id is required");
        }
        if (boardRepository != null && boardRepository.findById(boardId).isEmpty()) {
            throw new ValidationException("Board not found.");
        }
        BoardColumn col = requireColumn(boardColumnId);
        String trimmedTitle = requireTitle(title);
        String trimmedDesc = normalizeDescription(description);

        double position = nextPosition(boardColumnId);
        Instant now = Instant.now();
        Instant completedAt = col.isFinal() ? now : null;
        Card card = new Card(null, boardId, boardColumnId, trimmedTitle, trimmedDesc,
                position, dueDate, now, now, completedAt,
                priority != null ? priority : CardPriority.MEDIUM,
                severity != null ? severity : CardSeverity.MINOR,
                null);
        card.setChecklists(checklists != null ? new ArrayList<>(checklists) : new ArrayList<>());
        Card saved = cardRepository.insert(card);
        if (tagRepository != null && tagIds != null) {
            tagRepository.setCardTags(saved.getId(), tagIds);
            saved.setTags(tagRepository.findByCardId(saved.getId()));
        }
        if (checklistRepository != null) {
            checklistRepository.syncForCard(saved.getId(), saved.getChecklists());
        }
        logger.info("Created card '{}' (id={}) in column {}", saved.getTitle(), saved.getId(), boardColumnId);
        return saved;
    }

    public Card updateCard(long id, String title, String description,
            long boardColumnId, LocalDate dueDate) {
        Card existing = requireCard(id);
        return updateCard(id, title, description, boardColumnId, dueDate, existing.getPriority(),
                existing.getSeverity(), null);
    }

    public Card updateCard(long id, String title, String description,
            long boardColumnId, LocalDate dueDate, CardPriority priority, CardSeverity severity, List<Long> tagIds) {
        return updateCard(id, title, description, boardColumnId, dueDate, priority, severity, tagIds, null);
    }

    public Card updateCard(long id, String title, String description,
            long boardColumnId, LocalDate dueDate, CardPriority priority, CardSeverity severity,
            List<Long> tagIds, List<Checklist> checklists) {
        Card existing = requireCard(id);
        BoardColumn col = requireColumn(boardColumnId);
        String trimmedTitle = requireTitle(title);
        String trimmedDesc = normalizeDescription(description);

        boolean enteringFinal = col.isFinal()
                && (existing.getBoardColumnId() != boardColumnId || existing.getCompletedAt() == null);
        boolean leavingFinal = !col.isFinal();
        Instant completedAt;
        if (enteringFinal) {
            completedAt = Instant.now();
        } else if (leavingFinal) {
            completedAt = null;
        } else {
            completedAt = existing.getCompletedAt();
        }

        Instant now = Instant.now();
        Card updated = new Card(existing.getId(), existing.getBoardId(), boardColumnId,
                trimmedTitle, trimmedDesc, existing.getPosition(), dueDate,
                existing.getCreatedAt(), now, completedAt,
                priority != null ? priority : existing.getPriority(),
                severity != null ? severity : existing.getSeverity(),
                existing.getTags());
        updated.setChecklists(checklists != null ? new ArrayList<>(checklists) : new ArrayList<>());
        Card saved = cardRepository.update(updated);
        if (tagRepository != null && tagIds != null) {
            tagRepository.setCardTags(saved.getId(), tagIds);
            saved.setTags(tagRepository.findByCardId(saved.getId()));
        } else if (tagRepository != null) {
            saved.setTags(tagRepository.findByCardId(saved.getId()));
        }
        if (checklistRepository != null) {
            checklistRepository.syncForCard(saved.getId(), saved.getChecklists());
        }
        logger.info("Updated card '{}' (id={})", saved.getTitle(), saved.getId());
        return saved;
    }

    /**
     * Moves a card to the given column at the given position (1-based index
     * within the target column).
     */
    public Card moveCard(long cardId, long boardColumnId, int index) {
        Card existing = requireCard(cardId);
        requireColumn(boardColumnId);
        if (index < 1) {
            throw new ValidationException("Position must be at least 1");
        }

        long boardId = existing.getBoardId();
        List<Card> boardCards = cardRepository.findByBoard(boardId);

        List<Card> targetCards = new ArrayList<>(boardCards.stream()
                .filter(c -> c.getBoardColumnId() == boardColumnId && c.getId() != cardId)
                .toList());

        int insertionIndex = Math.max(0, Math.min(index - 1, targetCards.size()));
        targetCards.add(insertionIndex, existing);

        Instant now = Instant.now();
        boolean isTargetFinal = isFinalColumn(boardColumnId);
        boolean wasFinal = existing.getCompletedAt() != null;
        Instant completedAt = isTargetFinal ? (wasFinal ? existing.getCompletedAt() : now) : null;

        List<CardRepository.CardPlacement> placements = new ArrayList<>();
        for (int i = 0; i < targetCards.size(); i++) {
            Card c = targetCards.get(i);
            Instant comp = (c.getId() == cardId) ? completedAt : c.getCompletedAt();
            placements.add(new CardRepository.CardPlacement(c.getId(), boardColumnId, i + 1.0, now, comp));
        }

        if (existing.getBoardColumnId() != boardColumnId) {
            List<Card> sourceCards = boardCards.stream()
                    .filter(c -> c.getBoardColumnId() == existing.getBoardColumnId() && c.getId() != cardId)
                    .toList();
            for (int i = 0; i < sourceCards.size(); i++) {
                Card c = sourceCards.get(i);
                placements.add(new CardRepository.CardPlacement(c.getId(), c.getBoardColumnId(), i + 1.0, now,
                        c.getCompletedAt()));
            }
        }

        cardRepository.reorder(boardId, placements);
        logger.info("Moved card (id={}) to column {} at index {}", cardId, boardColumnId, index);
        return requireCard(cardId);
    }

    public boolean deleteCard(long id) {
        boolean deleted = cardRepository.delete(id);
        if (deleted) {
            logger.info("Deleted card (id={})", id);
        }
        return deleted;
    }

    /**
     * Returns the board's columns in display order.
     */
    public List<BoardColumn> getColumns(long boardId) {
        if (boardId <= 0) {
            throw new ValidationException("Board id is required");
        }
        return columnRepository.findByBoard(boardId);
    }

    /**
     * Moves all cards of the given column to the target column, appending
     * them in their current order.
     *
     * @return the number of cards that were moved
     */
    public int moveCardsToColumn(long sourceColumnId, long targetColumnId) {
        if (sourceColumnId == targetColumnId) {
            throw new ValidationException("Source and target columns must differ");
        }
        BoardColumn sourceCol = requireColumn(sourceColumnId);
        BoardColumn targetCol = requireColumn(targetColumnId);
        long boardId = sourceCol.getBoardId();

        List<Card> boardCards = cardRepository.findByBoard(boardId);
        List<Card> sourceCards = boardCards.stream()
                .filter(card -> card.getBoardColumnId() == sourceColumnId)
                .toList();
        if (sourceCards.isEmpty()) {
            return 0;
        }
        List<Card> targetCards = new ArrayList<>(boardCards.stream()
                .filter(card -> card.getBoardColumnId() == targetColumnId)
                .toList());

        Instant now = Instant.now();
        boolean isTargetFinal = targetCol.isFinal();

        List<CardRepository.CardPlacement> placements = new ArrayList<>();
        double startPos = targetCards.size() + 1.0;
        for (int i = 0; i < sourceCards.size(); i++) {
            Card c = sourceCards.get(i);
            Instant comp = isTargetFinal ? (c.getCompletedAt() != null ? c.getCompletedAt() : now) : null;
            placements.add(new CardRepository.CardPlacement(c.getId(), targetColumnId, startPos + i, now, comp));
        }
        cardRepository.reorder(boardId, placements);
        return sourceCards.size();
    }

    public Card requireCard(long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ValidationException("Card not found: " + cardId));
        if (checklistRepository != null) {
            card.setChecklists(checklistRepository.findByCardId(cardId));
        }
        return card;
    }

    /**
     * Loads a card including its checklists.
     */
    public Card getCard(long cardId) {
        return requireCard(cardId);
    }

    private BoardColumn requireColumn(long boardColumnId) {
        if (boardColumnId <= 0) {
            throw new ValidationException("Column is required");
        }
        return columnRepository.findById(boardColumnId)
                .orElseThrow(() -> new ValidationException("Column not found: " + boardColumnId));
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Card title is required.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException(
                    "Card title must be at most " + TITLE_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ValidationException(
                    "Card description must be at most " + DESCRIPTION_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }

    private boolean isFinalColumn(long boardColumnId) {
        return requireColumn(boardColumnId).isFinal();
    }

    private double nextPosition(long boardColumnId) {
        return countInColumn(boardColumnId) * POSITION_STEP + MIN_POSITION;
    }

    private long countInColumn(long boardColumnId) {
        long boardId = requireColumn(boardColumnId).getBoardId();
        return cardRepository.findByBoard(boardId).stream()
                .filter(card -> card.getBoardColumnId() == boardColumnId)
                .count();
    }
}
