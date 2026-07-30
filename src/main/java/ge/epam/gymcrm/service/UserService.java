package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.UserDAO;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UserDAO userDAO;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String firstName, String lastName) {
        String rawPassword = generatePassword();

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(generateUsername(firstName, lastName));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRawPassword(rawPassword);
        user.setActive(true);
        return user;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Failed password change attempt for user: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userDAO.update(user);
        log.info("Password changed for user: {}", username);
    }

    @Transactional(readOnly = true)
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
