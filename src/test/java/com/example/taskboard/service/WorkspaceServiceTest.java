package com.example.taskboard.service;

import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.Workspace;
import com.example.taskboard.repository.WorkspaceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for WorkspaceService using an in-memory workspace
 * repository, so no database is required.
 */
class WorkspaceServiceTest {

    private FakeWorkspaceRepository repository;
    private WorkspaceService service;

    @BeforeEach
    void setUp() {
        repository = new FakeWorkspaceRepository();
        service = new WorkspaceService(repository);
    }

    @Test
    void createSucceedsAndAssignsId() {
        Workspace created = service.create("Development", "Work projects");
        assertNotNull(created.getId());
        assertTrue(created.getId() > 0);
        assertEquals("Development", created.getName());
        assertEquals("Work projects", created.getDescription());
    }

    @Test
    void createTrimsNameAndDescription() {
        Workspace created = service.create("  Development  ", "  Work projects  ");
        assertEquals("Development", created.getName());
        assertEquals("Work projects", created.getDescription());
    }

    @Test
    void createStoresNullDescriptionWhenBlank() {
        Workspace created = service.create("Development", "   ");
        assertEquals("Development", created.getName());
        assertNull(created.getDescription());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(ValidationException.class, () -> service.create("   ", "description"));
        assertThrows(ValidationException.class, () -> service.create(null, "description"));
    }

    @Test
    void createRejectsNameLongerThanMaximum() {
        String tooLong = "w".repeat(WorkspaceService.NAME_MAX_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.create(tooLong, "description"));
    }

    @Test
    void createRejectsDescriptionLongerThanMaximum() {
        String tooLong = "d".repeat(WorkspaceService.DESCRIPTION_MAX_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.create("Development", tooLong));
    }

    @Test
    void updateChangesNameAndDescription() {
        Workspace created = service.create("Development", "Work projects");

        Workspace updated = service.update(created.getId(), "Renamed", "New description");
        assertEquals("Renamed", updated.getName());
        assertEquals("New description", updated.getDescription());
        assertTrue(!updated.getUpdatedAt().isBefore(created.getUpdatedAt()));

        Workspace reloaded = service.findById(created.getId()).orElseThrow();
        assertEquals("Renamed", reloaded.getName());
        assertEquals("New description", reloaded.getDescription());
    }

    @Test
    void updateRejectsBlankName() {
        Workspace created = service.create("Development", null);
        assertThrows(ValidationException.class, () -> service.update(created.getId(), "   ", "x"));
    }

    @Test
    void updateRejectsNameLongerThanMaximum() {
        Workspace created = service.create("Development", null);
        String tooLong = "w".repeat(WorkspaceService.NAME_MAX_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.update(created.getId(), tooLong, "x"));
    }

    @Test
    void updateFailsForUnknownId() {
        assertThrows(ValidationException.class, () -> service.update(999, "Ghost", null));
    }

    @Test
    void deleteRemovesExistingWorkspace() {
        Workspace created = service.create("Development", null);
        assertTrue(service.delete(created.getId()));
        assertTrue(service.findById(created.getId()).isEmpty());
    }

    @Test
    void deleteReturnsFalseForUnknownId() {
        assertFalse(service.delete(999));
    }

    @Test
    void findAllReturnsAllWorkspaces() {
        service.create("Development", null);
        service.create("Personal", null);
        List<Workspace> all = service.findAll();
        assertEquals(2, all.size());
    }

    /** Simple in-memory stand-in for WorkspaceRepository in unit tests. */
    private static final class FakeWorkspaceRepository implements WorkspaceRepository {

        private final Map<Long, Workspace> byId = new HashMap<>();
        private long nextId = 1;

        @Override
        public Workspace insert(Workspace workspace) {
            workspace.setId(nextId++);
            byId.put(workspace.getId(), workspace);
            return workspace;
        }

        @Override
        public Workspace update(Workspace workspace) {
            byId.put(workspace.getId(), workspace);
            return workspace;
        }

        @Override
        public Optional<Workspace> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Workspace> findAll() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public long count() {
            return byId.size();
        }

        @Override
        public int delete(long id) {
            return byId.remove(id) != null ? 1 : 0;
        }
    }
}
