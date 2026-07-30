package ge.epam.gymcrm.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789-abcdef";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void generatesTokenWhoseSubjectIsTheUsername() {
        String token = jwtService.generateToken("John.Doe");

        assertThat(jwtService.extractUsername(token)).isEqualTo("John.Doe");
        assertThat(jwtService.extractTokenId(token)).isNotBlank();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = new JwtService("another-secret-another-secret-0123456789-abcdef", 60)
                .generateToken("John.Doe");

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token = new JwtService(SECRET, -1).generateToken("John.Doe");

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("John.Doe");
        String tampered = token.substring(0, token.length() - 2);

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(RuntimeException.class);
    }
}
