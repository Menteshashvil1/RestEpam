package ge.epam.gymcrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.config.AuthenticationInterceptor;
import ge.epam.gymcrm.logging.TransactionContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
class GymCrmIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder,
                                             String username, String password) {
        return builder
                .header(AuthenticationInterceptor.USERNAME_HEADER, username)
                .header(AuthenticationInterceptor.PASSWORD_HEADER, password);
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    @Test
    void fullFlow() throws Exception {
        // 17. Training types are seeded and readable (after registering someone who can read them).
        String traineeBody = mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe",
                                 "dateOfBirth":"1995-04-23","address":"Tbilisi"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists(TransactionContext.TRANSACTION_ID_HEADER))
                .andReturn().getResponse().getContentAsString();

        String traineeUsername = json(traineeBody).get("username").asText();
        String traineePassword = json(traineeBody).get("password").asText();
        assertThat(traineeUsername).isEqualTo("John.Doe");
        assertThat(traineePassword).hasSize(10);

        // 3. Login works with the generated credentials.
        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", traineeUsername)
                        .param("password", traineePassword))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", traineeUsername)
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized());

        // 17. Training types.
        String typesBody = mockMvc.perform(as(get("/api/v1/training-types"), traineeUsername, traineePassword))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long cardioId = -1;
        for (JsonNode type : json(typesBody)) {
            if ("Cardio".equals(type.get("trainingType").asText())) {
                cardioId = type.get("trainingTypeId").asLong();
            }
        }
        assertThat(cardioId).isPositive();

        // 2. Trainer registration.
        String trainerBody = mockMvc.perform(post("/api/v1/trainers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Mary","lastName":"Smith","specializationId":%d}"""
                                .formatted(cardioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String trainerUsername = json(trainerBody).get("username").asText();
        String trainerPassword = json(trainerBody).get("password").asText();

        // Note 2: the same person cannot be both a trainer and a trainee.
        mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Mary","lastName":"Smith"}"""))
                .andExpect(status().isConflict());

        // 10. The new trainer is active and not yet assigned to the trainee.
        mockMvc.perform(as(get("/api/v1/trainees/" + traineeUsername + "/unassigned-trainers"),
                        traineeUsername, traineePassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainerUsername));

        // 11. Assign the trainer.
        mockMvc.perform(as(put("/api/v1/trainees/" + traineeUsername + "/trainers"),
                        traineeUsername, traineePassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trainerUsernames":["%s"]}""".formatted(trainerUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainerUsername))
                .andExpect(jsonPath("$[0].specialization.trainingType").value("Cardio"));

        // 5. The trainee profile now carries the trainer.
        mockMvc.perform(as(get("/api/v1/trainees/" + traineeUsername), traineeUsername, traineePassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.address").value("Tbilisi"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers[0].username").value(trainerUsername));

        // 14. Add a training.
        mockMvc.perform(as(post("/api/v1/trainings"), trainerUsername, trainerPassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"traineeUsername":"%s","trainerUsername":"%s",
                                 "trainingName":"Morning cardio","trainingDate":"2026-07-21",
                                 "trainingDuration":60}""".formatted(traineeUsername, trainerUsername)))
                .andExpect(status().isOk());

        // 12. Trainee trainings, filtered by period and training type.
        mockMvc.perform(as(get("/api/v1/trainees/" + traineeUsername + "/trainings"),
                        traineeUsername, traineePassword)
                        .param("periodFrom", "2026-01-01")
                        .param("periodTo", "2026-12-31")
                        .param("trainingType", "Cardio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning cardio"))
                .andExpect(jsonPath("$[0].trainerName").value("Mary Smith"))
                .andExpect(jsonPath("$[0].trainingDuration").value(60));

        // 13. Trainer trainings, filtered by trainee name.
        mockMvc.perform(as(get("/api/v1/trainers/" + trainerUsername + "/trainings"),
                        trainerUsername, trainerPassword)
                        .param("traineeName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traineeName").value("John Doe"));

        // 8. Trainer profile lists the trainee.
        mockMvc.perform(as(get("/api/v1/trainers/" + trainerUsername), trainerUsername, trainerPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization.trainingType").value("Cardio"))
                .andExpect(jsonPath("$.trainees[0].username").value(traineeUsername));

        // 6. Update the trainee profile.
        mockMvc.perform(as(put("/api/v1/trainees/" + traineeUsername), traineeUsername, traineePassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Johnny","lastName":"Doe","address":"Batumi","isActive":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(traineeUsername))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.address").value("Batumi"));

        // 9. Update the trainer profile — the specialization stays untouched.
        mockMvc.perform(as(put("/api/v1/trainers/" + trainerUsername), trainerUsername, trainerPassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Marianne","lastName":"Smith","isActive":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Marianne"))
                .andExpect(jsonPath("$.specialization.trainingType").value("Cardio"));

        // 15. De-activation works once, the repeat is rejected (not idempotent).
        mockMvc.perform(as(patch("/api/v1/trainees/" + traineeUsername + "/status"),
                        traineeUsername, traineePassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isActive":false}"""))
                .andExpect(status().isOk());

        mockMvc.perform(as(patch("/api/v1/trainees/" + traineeUsername + "/status"),
                        traineeUsername, traineePassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isActive":false}"""))
                .andExpect(status().isConflict());

        // 16. Same for the trainer.
        mockMvc.perform(as(patch("/api/v1/trainers/" + trainerUsername + "/status"),
                        trainerUsername, trainerPassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isActive":false}"""))
                .andExpect(status().isOk());

        // 4. Change login, then the old password stops working.
        mockMvc.perform(put("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","oldPassword":"%s","newPassword":"newSecret1"}"""
                                .formatted(traineeUsername, traineePassword)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", traineeUsername)
                        .param("password", traineePassword))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/login")
                        .param("username", traineeUsername)
                        .param("password", "newSecret1"))
                .andExpect(status().isOk());

        // 7. Hard delete cascades to the trainee's trainings.
        mockMvc.perform(as(delete("/api/v1/trainees/" + traineeUsername), traineeUsername, "newSecret1"))
                .andExpect(status().isOk());

        mockMvc.perform(as(get("/api/v1/trainers/" + trainerUsername + "/trainings"),
                        trainerUsername, trainerPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void secondRegistrationOfTheSameNameGetsASerialUsername() throws Exception {
        String first = mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Nick","lastName":"Jones"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Nick","lastName":"Jones"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(json(first).get("username").asText()).isEqualTo("Nick.Jones");
        assertThat(json(second).get("username").asText()).isEqualTo("Nick.Jones1");
    }

    @Test
    void unknownEndpointsAndUnauthenticatedCallsAreHandled() throws Exception {
        mockMvc.perform(get("/api/v1/trainees/Ghost"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.transactionId").exists());
    }
}
