package ge.epam.gymcrm.config;

import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    public static final String USERNAME_HEADER = "X-Auth-Username";
    public static final String PASSWORD_HEADER = "X-Auth-Password";

    private final UserService userService;

    @Autowired
    public AuthenticationInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String username = request.getHeader(USERNAME_HEADER);
        String password = request.getHeader(PASSWORD_HEADER);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationFailedException(
                    "Authentication required: provide the %s and %s headers"
                            .formatted(USERNAME_HEADER, PASSWORD_HEADER));
        }
        userService.authenticate(username, password);
        return true;
    }
}
