package com.example.taskboard.repository;

import com.example.taskboard.model.User;

import java.util.Optional;

/**
 * Data access for users.
 *
 * <p>Read operations plus insert (used by the development-only user
 * bootstrap). Password hashes are stored, never plaintext passwords.</p>
 */
public interface UserRepository {

    User insert(User user);

    Optional<User> findByUsername(String username);

    long count();
}
