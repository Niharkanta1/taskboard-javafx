package com.boardly.taskboard.service;

import com.boardly.taskboard.database.DatabaseManager;
import com.boardly.taskboard.exception.AuthenticationException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.User;
import com.boardly.taskboard.repository.UserRepository;
import com.boardly.taskboard.repository.impl.UserRepositoryImpl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the authentication flow against a temporary
 * SQLite database. The real application database is never touched.
 */
class AuthenticationIntegrationTest {

    private static Path tempDir;
    private static DatabaseManager db;
    private static UserRepository userRepository;
    private static AuthService authService;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("taskboard-auth-test");
        db = new DatabaseManager(tempDir.resolve("taskboard.db"));
        db.initialize();
        userRepository = new UserRepositoryImpl(db);
        authService = new AuthService(userRepository);
    }

    @AfterAll
    static void tearDown() throws IOException {
        db.close();
        deleteRecursively(tempDir);
    }

    @Test
    void devBootstrapCreatesInitialUserOnlyWhenEmpty() {
        long before = userRepository.count();
        new DevUserBootstrap(userRepository, authService).ensureInitialUser();
        assertEquals(before + 1, userRepository.count());

        // a second run must not create another user
        new DevUserBootstrap(userRepository, authService).ensureInitialUser();
        assertEquals(before + 1, userRepository.count());
    }

    @Test
    void loginSucceedsWithStoredHash() {
        User stored = new User("auth-test-user", authService.hashPassword("pw-456"));
        userRepository.insert(stored);

        User loggedIn = authService.login("auth-test-user", "pw-456");
        assertEquals(stored.getId(), loggedIn.getId());
        assertEquals("auth-test-user", loggedIn.getUsername());
    }

    @Test
    void loginFailsWithWrongPassword() {
        userRepository.insert(new User("wrong-pw-user", authService.hashPassword("pw-456")));
        assertThrows(AuthenticationException.class,
                () -> authService.login("wrong-pw-user", "not-the-password"));
    }

    @Test
    void loginFailsForUnknownUser() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("no-such-user", "pw-456"));
    }

    @Test
    void loginRejectsBlankInput() {
        assertThrows(ValidationException.class, () -> authService.login("", "pw-456"));
        assertThrows(ValidationException.class, () -> authService.login("somebody", ""));
    }

    @Test
    void storedHashIsNotThePlainPassword() {
        User stored = new User("hash-check", authService.hashPassword("pw-456"));
        userRepository.insert(stored);
        User found = userRepository.findByUsername("hash-check").orElseThrow();
        assertNotEquals("pw-456", found.getPasswordHash(), "password must be stored hashed");
    }

    private static void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // best effort cleanup of temp test directory
                }
            });
        }
    }
}
