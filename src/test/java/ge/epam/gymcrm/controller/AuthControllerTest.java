package ge.epam.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.config.AuthenticationInterceptor;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.dto.request.ChangePasswordRequest;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.logging.TransactionContext;
import ge.epam.gymcrm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthenticationInterceptor.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void loginReturnsOkForMatchingCredentials() throws Exception {
        when(userService.authenticate("John.Doe", "secret")).thenReturn(new User());

        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", "John.Doe")
                        .param("password", "secret"))
                .andExpect(status().isOk());
    }

    @Test
    void loginReturnsUnauthorizedForWrongCredentials() throws Exception {
        when(userService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationFailedException("Invalid username or password"));

        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", "John.Doe")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginRequiresThePasswordParameter() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login").param("username", "John.Doe"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void everyResponseCarriesTheTransactionId() throws Exception {
        when(userService.authenticate("John.Doe", "secret")).thenReturn(new User());

        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", "John.Doe")
                        .param("password", "secret"))
                .andExpect(header().exists(TransactionContext.TRANSACTION_ID_HEADER));
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
