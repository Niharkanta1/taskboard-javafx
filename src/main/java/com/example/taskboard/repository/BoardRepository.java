package com.example.taskboard.repository;

import com.example.taskboard.model.Board;

import java.util.List;
import java.util.Optional;

/**
 * Data access for boards.
 */
public interface BoardRepository {

    Board insert(Board board);

    Optional<Board> findById(long id);

    List<Board> findByWorkspace(long workspaceId);

    long count();
}
