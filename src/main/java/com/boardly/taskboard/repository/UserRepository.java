package com.boardly.taskboard.repository;

import com.boardly.taskboard.model.User;

import java.util.Optional;
import java.util.List;

/**
 * Data access for users.
 *
 * <p>
 * Read operations plus insert (used by the development-only user
 * bootstrap). Password hashes are stored, never plaintext passwords.
 * </p>
 */
public interface UserRepository {

    User insert(User user);

    Optional<User> findByUsername(String username);

    default List<String> findUsernames() {
        return List.of();
    }

    long count();
}
