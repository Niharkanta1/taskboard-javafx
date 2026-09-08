package com.example.taskboard.repository;

import com.example.taskboard.model.Workspace;

import java.util.List;
import java.util.Optional;

/**
 * Data access for workspaces.
 */
public interface WorkspaceRepository {

    Workspace insert(Workspace workspace);

    Workspace update(Workspace workspace);

    Optional<Workspace> findById(long id);

    List<Workspace> findAll();

    long count();

    /**
     * @return the number of deleted rows (0 or 1)
     */
    int delete(long id);
}
