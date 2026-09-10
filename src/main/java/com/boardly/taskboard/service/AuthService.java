package com.boardly.taskboard.service;

import org.mindrot.jbcrypt.BCrypt;

import com.boardly.taskboard.exception.AuthenticationException;
import com.boardly.taskboard.exception.AppException;
import com.boardly.taskboard.exception.DatabaseException;
import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.User;
import com.boardly.taskboard.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.List;

/**
 * Authentication service: validates credentials and verifies them against
 * the stored BCrypt hash.
 *
 * <p>
 * Plaintext passwords are never compared directly against database
 * values, never stored and never logged.
 * </p>
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final int BCRYPT_LOG_ROUNDS = 12;

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates the given credentials.
     *
     * @return the user on success
     * @throws ValidationException     if the username or password is blank
     * @throws AuthenticationException if the credentials do not match
     */
    public User login(String username, String password) {
        validateCredentials(username, password);

        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            logger.warn("Login failed for unknown username '{}'", username);
            throw new AuthenticationException("Invalid username or password.");
        }
        if (!verifyPassword(password, user.get().getPasswordHash())) {
            logger.warn("Login failed for user '{}': password mismatch", username);
            throw new AuthenticationException("Invalid username or password.");
        }
        logger.info("User '{}' authenticated successfully", username);
        return user.get();
    }

    /** Creates a user with a BCrypt password hash; plaintext is never persisted. */
    public User register(String username, String password, String confirmation) {
        String normalizedUsername = validateRegistration(username, password, confirmation);
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ValidationException("That username is already in use.");
        }
        try {
            User user = new User(normalizedUsername, hashPassword(password));
            User saved = userRepository.insert(user);
            logger.info("Created user account '{}'", normalizedUsername);
            return saved;
        } catch (DatabaseException e) {
            throw new AppException("Unable to create the user account.", e);
        }
    }

    public List<String> findUsernames() {
        return userRepository.findUsernames();
    }

    /**
     * Hashes a plaintext password with BCrypt. The raw password is never
     * stored or logged.
     */
    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password is required.");
        }
        String salt = BCrypt.gensalt(BCRYPT_LOG_ROUNDS);
        return BCrypt.hashpw(rawPassword, salt);
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     * Malformed hashes are treated as verification failures.
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            logger.warn("Password hash is malformed; treating verification as failed", e);
            return false;
        }
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required.");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required.");
        }
    }

    private String validateRegistration(String username, String password, String confirmation) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required.");
        }
        String normalized = username.trim();
        if (normalized.length() > 100) {
            throw new ValidationException("Username must be at most 100 characters.");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required.");
        }
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters.");
        }
        if (!password.equals(confirmation)) {
            throw new ValidationException("Passwords do not match.");
        }
        return normalized;
    }
}
