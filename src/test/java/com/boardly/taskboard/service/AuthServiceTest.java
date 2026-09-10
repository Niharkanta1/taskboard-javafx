package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.AuthenticationException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.User;
import com.boardly.taskboard.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for AuthService using an in-memory user repository,
 * so no database is required.
 */
class AuthServiceTest {

    private FakeUserRepository users;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        authService = new AuthService(users);
    }

    @Test
    void hashPasswordProducesHashDifferentFromPlainPassword() {
        String hash = authService.hashPassword("secret");
        assertNotNull(hash);
        assertNotEquals("secret", hash);
        // BCrypt hashes are salted: two hashes of the same password differ
        assertNotEquals(authService.hashPassword("secret"), hash);
    }

    @Test
    void hashPasswordRejectsBlankPassword() {
        assertThrows(ValidationException.class, () -> authService.hashPassword("   "));
    }

    @Test
    void verifyPasswordAcceptsCorrectPassword() {
        String hash = authService.hashPassword("secret");
        assertTrue(authService.verifyPassword("secret", hash));
    }

    @Test
    void verifyPasswordRejectsWrongPassword() {
        String hash = authService.hashPassword("secret");
        assertFalse(authService.verifyPassword("wrong", hash));
    }

    @Test
    void verifyPasswordRejectsMalformedHash() {
        assertFalse(authService.verifyPassword("secret", "not-a-bcrypt-hash"));
    }

    @Test
    void loginSucceedsForValidCredentials() {
        User stored = new User("alice", authService.hashPassword("pw-123"));
        users.put(stored);

        User loggedIn = authService.login("alice", "pw-123");
        assertEquals("alice", loggedIn.getUsername());
    }

    @Test
    void loginFailsForWrongPassword() {
        User stored = new User("alice", authService.hashPassword("pw-123"));
        users.put(stored);

        assertThrows(AuthenticationException.class,
                () -> authService.login("alice", "bad-password"));
    }

    @Test
    void loginFailsForUnknownUser() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("ghost", "any-password"));
    }

    @Test
    void loginRejectsBlankUsername() {
        assertThrows(ValidationException.class,
                () -> authService.login("   ", "pw-123"));
    }

    @Test
    void loginRejectsBlankPassword() {
        User stored = new User("alice", authService.hashPassword("pw-123"));
        users.put(stored);

        assertThrows(ValidationException.class,
                () -> authService.login("alice", "  "));
    }

    @Test
    void registerCreatesHashedUserThatCanLogIn() {
        User created = authService.register("  new-user  ", "strong-pass", "strong-pass");

        assertEquals("new-user", created.getUsername());
        assertNotEquals("strong-pass", created.getPasswordHash());
        assertEquals("new-user", authService.login("new-user", "strong-pass").getUsername());
    }

    @Test
    void registerRejectsDuplicateAndMismatchedOrWeakCredentials() {
        authService.register("new-user", "strong-pass", "strong-pass");

        assertThrows(ValidationException.class,
                () -> authService.register("new-user", "another-pass", "another-pass"));
        assertThrows(ValidationException.class,
                () -> authService.register("other-user", "short", "short"));
        assertThrows(ValidationException.class,
                () -> authService.register("other-user", "strong-pass", "different-pass"));
    }

    /** Simple in-memory stand-in for UserRepository in unit tests. */
    private static final class FakeUserRepository implements UserRepository {

        private final Map<String, User> byUsername = new HashMap<>();

        void put(User user) {
            byUsername.put(user.getUsername(), user);
        }

        @Override
        public User insert(User user) {
            byUsername.put(user.getUsername(), user);
            return user;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public long count() {
            return byUsername.size();
        }
    }
}
