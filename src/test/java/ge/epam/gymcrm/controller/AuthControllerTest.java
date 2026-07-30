package ge.epam.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.dto.request.ChangePasswordRequest;
import ge.epam.gymcrm.dto.request.LoginRequest;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.exception.TooManyAttemptsException;
import ge.epam.gymcrm.security.JwtService;
import ge.epam.gymcrm.security.TokenBlacklistService;
import ge.epam.gymcrm.service.AuthService;
import ge.epam.gymcrm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void loginReturnsTokenForMatchingCredentials() throws Exception {
        when(authService.login("John.Doe", "secret")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("John.Doe", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Doe"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void loginReturnsUnauthorizedForWrongCredentials() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new AuthenticationFailedException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("John.Doe", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginReturnsTooManyRequestsWhenBlocked() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new TooManyAttemptsException("Too many failed login attempts."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("John.Doe", "wrong"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void loginRequiresThePassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"John.Doe\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordReturnsOk() throws Exception {
        var request = new ChangePasswordRequest("John.Doe", "oldSecret", "newSecret1");

        mockMvc.perform(put("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).changePassword("John.Doe", "oldSecret", "newSecret1");
    }

    @Test
    void changePasswordRejectsTooShortNewPassword() throws Exception {
        var request = new ChangePasswordRequest("John.Doe", "oldSecret", "short");

        mockMvc.perform(put("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.newPassword").exists());
    }

    @Test
    void changePasswordReturnsUnauthorizedWhenOldPasswordIsWrong() throws Exception {
        doThrow(new AuthenticationFailedException("Invalid username or password"))
                .when(userService).changePassword(anyString(), anyString(), anyString());

        var request = new ChangePasswordRequest("John.Doe", "wrong", "newSecret1");

        mockMvc.perform(put("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
