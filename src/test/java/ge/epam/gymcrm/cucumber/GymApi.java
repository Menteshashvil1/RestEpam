package ge.epam.gymcrm.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.service.TrainingTypeService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class GymApi {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final TrainingTypeService trainingTypeService;

    public GymApi(MockMvc mockMvc, ObjectMapper objectMapper, TrainingTypeService trainingTypeService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.trainingTypeService = trainingTypeService;
    }

    public MvcResult registerTrainee(String firstName, String lastName) throws Exception {
        return mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"%s\",\"lastName\":\"%s\",\"address\":\"Tbilisi\"}"
                                .formatted(firstName, lastName)))
                .andReturn();
    }

    public MvcResult registerTrainer(String firstName, String lastName) throws Exception {
        return mockMvc.perform(post("/api/v1/trainers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"%s\",\"lastName\":\"%s\",\"specializationId\":%d}"
                                .formatted(firstName, lastName, anyTrainingTypeId())))
                .andReturn();
    }

    public MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andReturn();
    }

    public MvcResult addTraining(String token, String traineeUsername, String trainerUsername,
                                 String trainingName, LocalDate date, int duration) throws Exception {
        return mockMvc.perform(bearer(post("/api/v1/trainings"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"traineeUsername\":\"%s\",\"trainerUsername\":\"%s\","
                                + "\"trainingName\":\"%s\",\"trainingDate\":\"%s\","
                                + "\"trainingDuration\":%d}")
                                .formatted(traineeUsername, trainerUsername, trainingName, date, duration)))
                .andReturn();
    }

    public MvcResult deleteTraining(String token, long trainingId) throws Exception {
        return mockMvc.perform(bearer(delete("/api/v1/trainings/" + trainingId), token)).andReturn();
    }

    public MvcResult deleteTrainee(String token, String username) throws Exception {
        return mockMvc.perform(bearer(delete("/api/v1/trainees/" + username), token)).andReturn();
    }

    public MvcResult traineeTrainings(String token, String username) throws Exception {
        return mockMvc.perform(bearer(get("/api/v1/trainees/" + username + "/trainings"), token))
                .andReturn();
    }

    public MvcResult trainerProfile(String token, String username) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/trainers/" + username);
        return mockMvc.perform(token == null ? request : bearer(request, token)).andReturn();
    }

    public JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long anyTrainingTypeId() {
        return trainingTypeService.findAll().get(0).getId();
    }

    private MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
