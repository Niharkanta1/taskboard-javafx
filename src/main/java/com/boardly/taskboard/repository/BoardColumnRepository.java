package com.boardly.taskboard.repository;

import com.boardly.taskboard.model.BoardColumn;

import java.util.List;
import java.util.Optional;

/**
 * Persistence interface for board columns.
 */
public interface BoardColumnRepository {

    BoardColumn insert(BoardColumn column);

    Optional<BoardColumn> findById(long id);

    /**
     * All columns of the given board, ordered by position.
     */
    List<BoardColumn> findByBoard(long boardId);

    BoardColumn update(BoardColumn column);

    boolean delete(long id);

    long countByBoard(long boardId);
}
