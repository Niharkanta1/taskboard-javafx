package com.example.taskboard.session;

import com.example.taskboard.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the in-memory session lifecycle.
 */
class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    void noUserBeforeLogin() {
        assertFalse(sessionManager.isLoggedIn());
        assertTrue(sessionManager.getCurrentUser().isEmpty());
    }

    @Test
    void loginStoresCurrentUser() {
        User user = new User("alice", "hash");
        sessionManager.login(user);

        assertTrue(sessionManager.isLoggedIn());
        Optional<User> current = sessionManager.getCurrentUser();
        assertTrue(current.isPresent());
        assertEquals("alice", current.get().getUsername());
    }

    @Test
    void logoutClearsSession() {
        sessionManager.login(new User("alice", "hash"));
        sessionManager.logout();

        assertFalse(sessionManager.isLoggedIn());
        assertTrue(sessionManager.getCurrentUser().isEmpty());
    }

    @Test
    void logoutWithoutLoginIsSafe() {
        sessionManager.logout();
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    void loginRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> sessionManager.login(null));
    }
}
