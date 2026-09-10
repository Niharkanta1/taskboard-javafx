package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Board;
import com.boardly.taskboard.repository.BoardRepository;
import com.boardly.taskboard.repository.CardRepository;
import com.boardly.taskboard.repository.WorkspaceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Board business rules: validation, creation and loading of boards
 * together with their cards.
 *
 * <p>Controllers call this service instead of touching the repositories
 * directly, so validation and business rules stay in one place.</p>
 */
public class BoardService {

    private static final Logger logger = LoggerFactory.getLogger(BoardService.class);

    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final WorkspaceRepository workspaceRepository;

    public BoardService(BoardRepository boardRepository,
                        CardRepository cardRepository,
                        WorkspaceRepository workspaceRepository) {
        this.boardRepository = boardRepository;
        this.cardRepository = cardRepository;
        this.workspaceRepository = workspaceRepository;
    }

    /**
     * Creates a board inside the given workspace.
     *
     * @throws ValidationException if the workspace does not exist or the
     *                             name is missing or too long
     */
    public Board createBoard(long workspaceId, String name, String description) {
        if (workspaceRepository.findById(workspaceId).isEmpty()) {
            throw new ValidationException("Workspace not found.");
        }
        Board board = new Board(workspaceId, normalizeName(name), normalizeDescription(description));
        Board saved = boardRepository.insert(board);
        logger.info("Created board '{}' (id={}) in workspace {}", saved.getName(), saved.getId(), workspaceId);
        return saved;
    }

    /**
     * Updates an existing board.
     *
     * @throws ValidationException if the board does not exist or the
     *                             name is missing or too long
     */
    public Board updateBoard(long id, String name, String description) {
        String trimmedName = normalizeName(name);
        String trimmedDescription = normalizeDescription(description);

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Board not found."));
        board.setName(trimmedName);
        board.setDescription(trimmedDescription);
        board.setUpdatedAt(Instant.now());

        Board saved = boardRepository.update(board);
        logger.info("Updated board '{}' (id={})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Deletes a board. Foreign-key cascade removes its cards and attachments.
     *
     * @return true if a board was deleted, false if no board with the
     *         given id existed
     */
    public boolean delete(long id) {
        boolean deleted = boardRepository.delete(id) > 0;
        if (deleted) {
            logger.info("Deleted board (id={}) including its cards", id);
        }
        return deleted;
    }

    /**
     * Loads a board together with all of its cards (ordered by position).
     *
     * @throws ValidationException if the board does not exist
     */
    public Board loadBoard(long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ValidationException("Board not found."));
        board.setCards(cardRepository.findByBoard(boardId));
        return board;
    }

    /**
     * Lists all boards that belong to the given workspace.
     */
    public List<Board> findBoardsByWorkspace(long workspaceId) {
        return boardRepository.findByWorkspace(workspaceId);
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
