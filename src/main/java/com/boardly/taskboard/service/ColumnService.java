package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.BoardColumn;
import com.boardly.taskboard.repository.BoardColumnRepository;
import com.boardly.taskboard.repository.CardRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for board columns: creation, renaming, reordering,
 * and deletion.
 *
 * <p>
 * A board must always keep at least {@link BoardService#MIN_COLUMNS_PER_BOARD}
 * columns, and a column can only be deleted after its cards have been
 * moved to another column.
 * </p>
 */
public class ColumnService {

    private final BoardColumnRepository columnRepository;
    private final CardRepository cardRepository;
    private final CardService cardService;

    public ColumnService(BoardColumnRepository columnRepository,
            CardRepository cardRepository,
            CardService cardService) {
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.cardService = cardService;
    }

    /**
     * Appends a new column to the end of the board.
     */
    public BoardColumn createColumn(long boardId, String name, boolean isFinal) {
        if (boardId <= 0) {
            throw new ValidationException("Board id is required");
        }
        String trimmedName = requireColumnName(name);
        double position = columnRepository.countByBoard(boardId) + 1.0;
        Instant now = Instant.now();
        return columnRepository.insert(new BoardColumn(null, boardId, trimmedName, position, isFinal, now, now));
    }

    /**
     * Marks a column as final (or not final).
     */
    public BoardColumn setColumnFinal(long id, boolean isFinal) {
        BoardColumn existing = requireColumn(id);
        Instant now = Instant.now();
        return columnRepository.update(new BoardColumn(existing.getId(), existing.getBoardId(),
                existing.getName(), existing.getPosition(), isFinal,
                existing.getCreatedAt(), now));
    }

    public BoardColumn renameColumn(long id, String name) {
        BoardColumn existing = requireColumn(id);
        String trimmedName = requireColumnName(name);
        Instant now = Instant.now();
        return columnRepository.update(new BoardColumn(existing.getId(), existing.getBoardId(),
                trimmedName, existing.getPosition(), existing.isFinal(),
                existing.getCreatedAt(), now));
    }

    /**
     * Moves a column to the given position (1-based index within the board).
     */
    public BoardColumn moveColumn(long id, int index) {
        BoardColumn existing = requireColumn(id);
        if (index < 1) {
            throw new ValidationException("Column position must be at least 1");
        }
        long boardId = existing.getBoardId();
        List<BoardColumn> columns = columnRepository.findByBoard(boardId);
        if (index > columns.size()) {
            throw new ValidationException("Column position exceeds the board size");
        }
        columns.removeIf(c -> c.getId().equals(existing.getId()));
        columns.add(index - 1, existing);
        for (int i = 0; i < columns.size(); i++) {
            columnRepository.update(new BoardColumn(columns.get(i).getId(), columns.get(i).getBoardId(),
                    columns.get(i).getName(), i + 1.0, columns.get(i).isFinal(),
                    columns.get(i).getCreatedAt(), columns.get(i).getUpdatedAt()));
        }
        return requireColumn(id);
    }

    /**
     * Deletes a column after moving all of its cards to the target column.
     *
     * @return the number of cards that were moved
     */
    public int deleteColumn(long id, Long targetColumnId) {
        BoardColumn existing = requireColumn(id);
        long boardId = existing.getBoardId();
        if (columnRepository.countByBoard(boardId) <= BoardService.MIN_COLUMNS_PER_BOARD) {
            throw new ValidationException(
                    "A board must keep at least " + BoardService.MIN_COLUMNS_PER_BOARD + " columns");
        }
        long cardCount = countCardsInColumn(id);
        int moved = 0;
        if (cardCount > 0) {
            if (targetColumnId == null || targetColumnId <= 0) {
                throw new ValidationException("A target column must be selected to move existing cards to");
            }
            if (id == targetColumnId.longValue()) {
                throw new ValidationException("Target column must be different from the deleted column");
            }
            requireColumn(targetColumnId);
            moved = cardService.moveCardsToColumn(id, targetColumnId);
        } else if (targetColumnId != null && targetColumnId > 0 && targetColumnId != id) {
            requireColumn(targetColumnId);
            moved = cardService.moveCardsToColumn(id, targetColumnId);
        }
        columnRepository.delete(id);
        return moved;
    }

    public long countCardsInColumn(long columnId) {
        BoardColumn column = requireColumn(columnId);
        return cardRepository.findByBoard(column.getBoardId()).stream()
                .filter(card -> card.getBoardColumnId() == columnId)
                .count();
    }

    public List<BoardColumn> getColumns(long boardId) {
        if (boardId <= 0) {
            throw new ValidationException("Board id is required");
        }
        return columnRepository.findByBoard(boardId);
    }

    public Optional<BoardColumn> findById(long id) {
        return columnRepository.findById(id);
    }

    private BoardColumn requireColumn(long id) {
        if (id <= 0) {
            throw new ValidationException("Column id is required");
        }
        return columnRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Column not found: " + id));
    }

    private String requireColumnName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Column name must not be empty");
        }
        return trimmed;
    }
}
