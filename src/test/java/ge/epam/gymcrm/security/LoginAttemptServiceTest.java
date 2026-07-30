package ge.epam.gymcrm.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-30T10:00:00Z"));
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    void blocksAfterThreeFailures() {
        service.loginFailed("John.Doe");
        service.loginFailed("John.Doe");
        assertThat(service.isBlocked("John.Doe")).isFalse();

        service.loginFailed("John.Doe");
        assertThat(service.isBlocked("John.Doe")).isTrue();
    }

    @Test
    void unblocksAfterFiveMinutes() {
        service.loginFailed("John.Doe");
        service.loginFailed("John.Doe");
        service.loginFailed("John.Doe");
        assertThat(service.isBlocked("John.Doe")).isTrue();

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertThat(service.isBlocked("John.Doe")).isFalse();
    }

    @Test
    void successResetsTheCounter() {
        service.loginFailed("John.Doe");
        service.loginFailed("John.Doe");
        service.loginSucceeded("John.Doe");

        service.loginFailed("John.Doe");
        service.loginFailed("John.Doe");
        assertThat(service.isBlocked("John.Doe")).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
