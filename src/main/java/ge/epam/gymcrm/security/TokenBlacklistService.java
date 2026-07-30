package ge.epam.gymcrm.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Clock clock;
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(Clock clock) {
        this.clock = clock;
    }

    public void blacklist(String tokenId, Instant expiresAt) {
        blacklist.put(tokenId, expiresAt);
    }

    public boolean isBlacklisted(String tokenId) {
        purgeExpired();
        return blacklist.containsKey(tokenId);
    }

    private void purgeExpired() {
        Instant now = Instant.now(clock);
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
