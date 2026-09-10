package com.boardly.taskboard.repository;

import com.boardly.taskboard.model.Board;

import java.util.List;
import java.util.Optional;

/**
 * Data access for boards.
 */
public interface BoardRepository {

    Board insert(Board board);

    Optional<Board> findById(long id);

    List<Board> findByWorkspace(long workspaceId);

    Board update(Board board);

    /**
     * @return the number of deleted rows (0 or 1)
     */
    int delete(long id);

    long count();
}
