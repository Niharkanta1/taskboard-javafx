package com.example.taskboard.session;

import com.example.taskboard.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * In-memory session for the currently signed-in user.
 *
 * <p>The session lives for one application run; the authoritative user
 * data is stored in the database.</p>
 */
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private User currentUser;

    public void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        currentUser = user;
        logger.info("Session opened for user '{}'", user.getUsername());
    }

    public void logout() {
        if (currentUser != null) {
            logger.info("Session closed for user '{}'", currentUser.getUsername());
        }
        currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
