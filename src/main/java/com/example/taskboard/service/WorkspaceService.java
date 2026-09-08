package com.example.taskboard.service;

import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.repository.WorkspaceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Workspace business rules: validation and CRUD operations.
 *
 * <p>Controllers call this service instead of touching the repository
 * directly, so validation and business rules stay in one place.</p>
 */
public class WorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceService.class);

    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private final WorkspaceRepository repository;

    public WorkspaceService(WorkspaceRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a workspace from the given (already user-visible) values.
     *
     * @throws ValidationException if the name is missing or too long
     */
    public Workspace create(String name, String description) {
        String trimmedName = normalizeName(name);
        String trimmedDescription = normalizeDescription(description);
        Workspace workspace = new Workspace(trimmedName, trimmedDescription);
        Workspace saved = repository.insert(workspace);
        logger.info("Created workspace '{}' (id={})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Updates an existing workspace.
     *
     * @throws ValidationException if the workspace does not exist or the
     *                             name is missing or too long
     */
    public Workspace update(long id, String name, String description) {
        String trimmedName = normalizeName(name);
        String trimmedDescription = normalizeDescription(description);

        Workspace workspace = repository.findById(id)
                .orElseThrow(() -> new ValidationException("Workspace not found."));
        workspace.setName(trimmedName);
        workspace.setDescription(trimmedDescription);
        workspace.setUpdatedAt(Instant.now());

        Workspace saved = repository.update(workspace);
        logger.info("Updated workspace '{}' (id={})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Deletes a workspace. Foreign-key cascade removes its boards and cards.
     *
     * @return true if a workspace was deleted, false if no workspace with
     *         the given id existed
     */
    public boolean delete(long id) {
        boolean deleted = repository.delete(id) > 0;
        if (deleted) {
            logger.info("Deleted workspace (id={}) including its boards and cards", id);
        }
        return deleted;
    }

    public List<Workspace> findAll() {
        return repository.findAll();
    }

    public Optional<Workspace> findById(long id) {
        return repository.findById(id);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Workspace name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new ValidationException(
                    "Workspace name must be at most " + NAME_MAX_LENGTH + " characters.");
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
                    "Workspace description must be at most " + DESCRIPTION_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }
}
