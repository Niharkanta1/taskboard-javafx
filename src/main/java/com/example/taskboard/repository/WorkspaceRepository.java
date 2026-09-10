package com.example.taskboard.repository;

import com.example.taskboard.model.Workspace;

import java.util.List;
import java.util.Optional;
import java.util.List;

/**
 * Data access for workspaces.
 */
public interface WorkspaceRepository {

    Workspace insert(Workspace workspace);

    Workspace update(Workspace workspace);

    Optional<Workspace> findById(long id);

    List<Workspace> findAll();

    default List<Workspace> findAllByOwner(long ownerUserId) {
        return findAll().stream().filter(workspace -> workspace.getOwnerUserId() == ownerUserId).toList();
    }

    default Optional<Workspace> findByIdAndOwner(long id, long ownerUserId) {
        return findById(id).filter(workspace -> workspace.getOwnerUserId() == ownerUserId);
    }

    long count();

    /**
     * @return the number of deleted rows (0 or 1)
     */
    int delete(long id);
}
