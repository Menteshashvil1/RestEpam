package ge.epam.gymcrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.logging.TransactionContext;
import org.junit.jupiter.api.Test;
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
class GymCrmIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    @Test
    void fullFlow() throws Exception {
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
        String traineeToken = json(traineeBody).get("token").asText();
        assertThat(traineeUsername).isEqualTo("John.Doe");
        assertThat(traineePassword).hasSize(10);
        assertThat(traineeToken).isNotBlank();

        mockMvc.perform(get("/api/v1/trainees/" + traineeUsername))
                .andExpect(status().isUnauthorized());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(traineeUsername, traineePassword)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json(loginBody).get("token").asText()).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"wrong"}""".formatted(traineeUsername)))
                .andExpect(status().isUnauthorized());

        String typesBody = mockMvc.perform(bearer(get("/api/v1/training-types"), traineeToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long cardioId = -1;
        for (JsonNode type : json(typesBody)) {
            if ("Cardio".equals(type.get("trainingType").asText())) {
                cardioId = type.get("trainingTypeId").asLong();
            }
        }
        assertThat(cardioId).isPositive();

        String trainerBody = mockMvc.perform(post("/api/v1/trainers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Mary","lastName":"Smith","specializationId":%d}"""
                                .formatted(cardioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String trainerUsername = json(trainerBody).get("username").asText();
        String trainerToken = json(trainerBody).get("token").asText();

        mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Mary","lastName":"Smith"}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(bearer(get("/api/v1/trainees/" + traineeUsername + "/unassigned-trainers"),
                        traineeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainerUsername));

        mockMvc.perform(bearer(put("/api/v1/trainees/" + traineeUsername + "/trainers"), traineeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trainerUsernames":["%s"]}""".formatted(trainerUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainerUsername))
                .andExpect(jsonPath("$[0].specialization.trainingType").value("Cardio"));

        mockMvc.perform(bearer(get("/api/v1/trainees/" + traineeUsername), traineeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.address").value("Tbilisi"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers[0].username").value(trainerUsername));

        mockMvc.perform(bearer(post("/api/v1/trainings"), trainerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"traineeUsername":"%s","trainerUsername":"%s",
                                 "trainingName":"Morning cardio","trainingDate":"2026-07-21",
                                 "trainingDuration":60}""".formatted(traineeUsername, trainerUsername)))
                .andExpect(status().isOk());

        mockMvc.perform(bearer(get("/api/v1/trainees/" + traineeUsername + "/trainings"), traineeToken)
                        .param("periodFrom", "2026-01-01")
                        .param("periodTo", "2026-12-31")
                        .param("trainingType", "Cardio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning cardio"))
                .andExpect(jsonPath("$[0].trainerName").value("Mary Smith"))
                .andExpect(jsonPath("$[0].trainingDuration").value(60));

        mockMvc.perform(bearer(get("/api/v1/trainers/" + trainerUsername + "/trainings"), trainerToken)
                        .param("traineeName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traineeName").value("John Doe"));

        mockMvc.perform(bearer(get("/api/v1/trainers/" + trainerUsername), trainerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization.trainingType").value("Cardio"))
                .andExpect(jsonPath("$.trainees[0].username").value(traineeUsername));

        mockMvc.perform(bearer(put("/api/v1/trainees/" + traineeUsername), traineeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Johnny","lastName":"Doe","address":"Batumi","isActive":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.address").value("Batumi"));

        mockMvc.perform(bearer(put("/api/v1/trainers/" + trainerUsername), trainerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Marianne","lastName":"Smith","isActive":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Marianne"))
                .andExpect(jsonPath("$.specialization.trainingType").value("Cardio"));

        mockMvc.perform(bearer(put("/api/v1/auth/login"), traineeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","oldPassword":"%s","newPassword":"newSecret1"}"""
                                .formatted(traineeUsername, traineePassword)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(traineeUsername, traineePassword)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"newSecret1"}""".formatted(traineeUsername)))
                .andExpect(status().isOk());

        mockMvc.perform(bearer(delete("/api/v1/trainees/" + traineeUsername), traineeToken))
                .andExpect(status().isOk());

        mockMvc.perform(bearer(get("/api/v1/trainers/" + trainerUsername + "/trainings"), trainerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(bearer(patch("/api/v1/trainers/" + trainerUsername + "/status"), trainerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isActive":false}"""))
                .andExpect(status().isOk());

        mockMvc.perform(bearer(get("/api/v1/trainers/" + trainerUsername), trainerToken))
                .andExpect(status().isUnauthorized());
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
    void unauthenticatedCallsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/trainees/Ghost"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.transactionId").exists());
    }
}
