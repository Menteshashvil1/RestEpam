package ge.epam.gymcrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode register(String firstName, String lastName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"%s"}""".formatted(firstName, lastName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void login(String username, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void blocksUserAfterThreeFailedLogins() throws Exception {
        JsonNode account = register("Bruce", "Force");
        String username = account.get("username").asText();
        String password = account.get("password").asText();

        login(username, "wrong", 401);
        login(username, "wrong", 401);
        login(username, "wrong", 401);

        login(username, password, 429);
    }

    @Test
    void logoutInvalidatesTheToken() throws Exception {
        JsonNode account = register("Log", "Out");
        String username = account.get("username").asText();
        String token = account.get("token").asText();

        mockMvc.perform(get("/api/v1/trainees/" + username)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trainees/" + username)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void registrationIsOpenButOtherEndpointsAreNot() throws Exception {
        JsonNode account = register("Open", "Gate");
        assertThat(account.get("token").asText()).isNotBlank();

        mockMvc.perform(get("/api/v1/trainees/" + account.get("username").asText()))
                .andExpect(status().isUnauthorized());
    }
}
