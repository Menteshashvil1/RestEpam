package ge.epam.gymcrm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    static final int MAX_ATTEMPTS = 3;
    static final int BLOCK_MINUTES = 5;

    private final Clock clock;
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String username) {
        Attempts current = attempts.get(username);
        if (current == null || current.blockedUntil == null) {
            return false;
        }
        if (Instant.now(clock).isBefore(current.blockedUntil)) {
            return true;
        }
        attempts.remove(username);
        return false;
    }

    public void loginFailed(String username) {
        Attempts current = attempts.compute(username, (key, existing) -> {
            Attempts value = (existing == null) ? new Attempts() : existing;
            value.count++;
            if (value.count >= MAX_ATTEMPTS) {
                value.blockedUntil = Instant.now(clock).plus(Duration.ofMinutes(BLOCK_MINUTES));
            }
            return value;
        });
        if (current.blockedUntil != null) {
            log.warn("User {} blocked until {} after {} failed logins",
                    username, current.blockedUntil, current.count);
        }
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
    }

    private static final class Attempts {
        private int count;
        private Instant blockedUntil;
    }
}
