package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.model.Card;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.WorkspaceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business rules for boards: validation, persistence, and loading.
 *
 * <p>
 * New boards are seeded with the four default columns (Planned,
 * In Progress, Completed, Closed); Completed and Closed are final
 * columns.
 * </p>
 */
public class BoardService {

    private static final Logger logger = LoggerFactory.getLogger(BoardService.class);

    public static final int MIN_COLUMNS_PER_BOARD = 3;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    public static final String DEFAULT_COLUMN_PLANNED = "Planned";
    public static final String DEFAULT_COLUMN_IN_PROGRESS = "In Progress";
    public static final String DEFAULT_COLUMN_COMPLETED = "Completed";
    public static final String DEFAULT_COLUMN_CLOSED = "Closed";

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final BoardColumnRepository columnRepository;
    private final WorkspaceRepository workspaceRepository;

    public BoardService(BoardRepository boardRepository,
            CardRepository cardRepository,
            BoardColumnRepository columnRepository) {
        this(boardRepository, cardRepository, columnRepository, null);
    }

    public BoardService(BoardRepository boardRepository,
            CardRepository cardRepository,
            BoardColumnRepository columnRepository,
            WorkspaceRepository workspaceRepository) {
        this.boardRepository = boardRepository;
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public Board createBoard(long workspaceId, String name, String description) {
        if (workspaceId <= 0) {
            throw new ValidationException("Workspace id is required");
        }
        if (workspaceRepository != null && workspaceRepository.findById(workspaceId).isEmpty()) {
            throw new ValidationException("Workspace not found.");
        }
        String trimmedName = normalizeName(name);
        String trimmedDescription = normalizeDescription(description);

        Board board = new Board(workspaceId, trimmedName, trimmedDescription);
        Board saved = boardRepository.insert(board);
        if (columnRepository != null) {
            seedDefaultColumns(saved.getId(), saved.getCreatedAt());
        }
        logger.info("Created board '{}' (id={}) in workspace {}", saved.getName(), saved.getId(), workspaceId);
        return saved;
    }

    public Board updateBoard(long id, String name, String description) {
        Board existing = boardRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Board not found."));
        String trimmedName = normalizeName(name);
        String trimmedDescription = normalizeDescription(description);

        existing.setName(trimmedName);
        existing.setDescription(trimmedDescription);
        existing.setUpdatedAt(Instant.now());

        Board updated = boardRepository.update(existing);
        logger.info("Updated board '{}' (id={})", updated.getName(), updated.getId());
        return updated;
    }

    public boolean delete(long id) {
        return deleteBoard(id);
    }

    public boolean deleteBoard(long id) {
        boolean deleted = boardRepository.delete(id) > 0;
        if (deleted) {
            logger.info("Deleted board (id={}) including its columns and cards", id);
        }
        return deleted;
    }

    public Board loadBoard(long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Board not found."));
        List<BoardColumn> columns = columnRepository != null ? columnRepository.findByBoard(id) : List.of();
        Map<Long, BoardColumn> columnsById = new HashMap<>();
        for (BoardColumn column : columns) {
            columnsById.put(column.getId(), column);
        }
        List<Card> cards = cardRepository != null ? cardRepository.findByBoard(id) : List.of();
        for (Card card : cards) {
            card.setColumn(columnsById.get(card.getBoardColumnId()));
        }
        board.setColumns(columns);
        board.setCards(cards);
        return board;
    }

    public List<Board> findBoardsByWorkspace(long workspaceId) {
        return boardRepository.findByWorkspace(workspaceId);
    }

    private void seedDefaultColumns(long boardId, Instant now) {
        columnRepository.insert(new BoardColumn(null, boardId, DEFAULT_COLUMN_PLANNED, 1.0, false, now, now));
        columnRepository.insert(new BoardColumn(null, boardId, DEFAULT_COLUMN_IN_PROGRESS, 2.0, false, now, now));
        columnRepository.insert(new BoardColumn(null, boardId, DEFAULT_COLUMN_COMPLETED, 3.0, true, now, now));
        columnRepository.insert(new BoardColumn(null, boardId, DEFAULT_COLUMN_CLOSED, 4.0, true, now, now));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Board name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new ValidationException(
                    "Board name must be at most " + NAME_MAX_LENGTH + " characters.");
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
                    "Board description must be at most " + DESCRIPTION_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }
}
