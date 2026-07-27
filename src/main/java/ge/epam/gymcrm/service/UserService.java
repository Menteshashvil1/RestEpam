package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.UserDAO;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/**
 * Credential concerns shared by trainees and trainers: username generation, password
 * generation and username/password matching.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UserDAO userDAO;
    private GymMetrics metrics;

    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Autowired
    public void setMetrics(GymMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Builds a new user with the credentials generated as described in the previous modules:
     * username is {@code firstName.lastName}, suffixed with a serial number when taken.
     */
    public User createUser(String firstName, String lastName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(generateUsername(firstName, lastName));
        user.setPassword(generatePassword());
        user.setActive(true);
        return user;
    }

    /** Verifies that the username exists and the password matches. */
    public User authenticate(String username, String password) {
        User user = userDAO.findByUsername(username)
                .orElseGet(() -> {
                    metrics.recordAuthenticationFailure();
                    throw new AuthenticationFailedException("Invalid username or password");
                });
        if (!user.getPassword().equals(password)) {
            metrics.recordAuthenticationFailure();
            log.warn("Failed authentication attempt for user: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }
        metrics.recordAuthenticationSuccess();
        return user;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = authenticate(username, oldPassword);
        user.setPassword(newPassword);
        userDAO.update(user);
        log.info("Password changed for user: {}", username);
    }

    public boolean usernameExists(String username) {
        return userDAO.findByUsername(username).isPresent();
    }

    private String generateUsername(String firstName, String lastName) {
        String base = firstName + "." + lastName;
        Set<String> taken = new HashSet<>(userDAO.findUsernamesStartingWith(base));
        if (!taken.contains(base)) {
            return base;
        }
        int serial = 1;
        while (taken.contains(base + serial)) {
            serial++;
        }
        return base + serial;
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
