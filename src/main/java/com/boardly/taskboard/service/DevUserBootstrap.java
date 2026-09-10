package com.boardly.taskboard.service;

import com.boardly.taskboard.model.User;
import com.boardly.taskboard.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only bootstrap that creates the initial user when the
 * database contains no users yet.
 *
 * <p>
 * This is a controlled development mechanism, not a production
 * feature: it runs once at startup and only when the users table is
 * empty. The development password is a documented default that can be
 * overridden with the {@code TASKBOARD_DEV_PASSWORD} environment
 * variable. No production password is hard-coded.
 * </p>
 */
public final class DevUserBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(DevUserBootstrap.class);

    public static final String DEV_USERNAME = "dev";
    public static final String PASSWORD_ENV_VAR = "TASKBOARD_DEV_PASSWORD";

    /**
     * Development-only default password; override with {@link #PASSWORD_ENV_VAR}.
     */
    static final String DEFAULT_DEV_PASSWORD = "Dev-1234";

    private final UserRepository userRepository;
    private final AuthService authService;

    public DevUserBootstrap(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Creates the initial development user if no user exists yet.
     */
    public void ensureInitialUser() {
        if (userRepository.count() > 0) {
            return;
        }
        String password = resolveDevelopmentPassword();
        User user = new User(DEV_USERNAME, authService.hashPassword(password));
        userRepository.insert(user);
        logger.info("Created development-only initial user '{}'. "
                + "Override the development password with the {} environment variable.",
                DEV_USERNAME, PASSWORD_ENV_VAR);
    }

    private String resolveDevelopmentPassword() {
        String fromEnv = System.getenv(PASSWORD_ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_DEV_PASSWORD;
    }
}
