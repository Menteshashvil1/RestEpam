package ge.epam.gymcrm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtLogoutHandler.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtLogoutHandler(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return;
        }
        try {
            Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()));
            tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());
            log.info("User {} logged out; token {} blacklisted", claims.getSubject(), claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Logout with invalid token ignored: {}", e.getMessage());
        }
    }
}
