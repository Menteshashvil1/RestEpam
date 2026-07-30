package ge.epam.gymcrm.service;

import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.exception.TooManyAttemptsException;
import ge.epam.gymcrm.metrics.GymMetrics;
import ge.epam.gymcrm.security.JwtService;
import ge.epam.gymcrm.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final GymMetrics metrics;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService,
                       GymMetrics metrics) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.metrics = metrics;
    }

    public String login(String username, String password) {
        if (loginAttemptService.isBlocked(username)) {
            log.warn("Blocked login attempt for {}", username);
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again in a few minutes.");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            loginAttemptService.loginFailed(username);
            metrics.recordAuthenticationFailure();
            log.warn("Failed authentication attempt for user: {}", username);
            throw new AuthenticationFailedException("Invalid username or password");
        }
        loginAttemptService.loginSucceeded(username);
        metrics.recordAuthenticationSuccess();
        log.info("User {} authenticated", username);
        return jwtService.generateToken(username);
    }

    public String issueToken(String username) {
        return jwtService.generateToken(username);
    }
}
