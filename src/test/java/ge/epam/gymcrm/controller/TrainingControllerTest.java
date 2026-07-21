package ge.epam.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.config.AuthenticationInterceptor;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.dto.request.AddTrainingRequest;
import ge.epam.gymcrm.dto.response.TrainingTypeResponse;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.exception.NotFoundException;
import ge.epam.gymcrm.mapper.GymMapper;
import ge.epam.gymcrm.service.TrainingService;
import ge.epam.gymcrm.service.TrainingTypeService;
import ge.epam.gymcrm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TrainingController.class, TrainingTypeController.class})
@Import({AuthenticationInterceptor.class, GymMapper.class})
class TrainingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingService trainingService;

    @MockBean
    private TrainingTypeService trainingTypeService;

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.authenticate(anyString(), anyString())).thenReturn(new User());
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder builder) {
        return builder
                .header(AuthenticationInterceptor.USERNAME_HEADER, "John.Doe")
                .header(AuthenticationInterceptor.PASSWORD_HEADER, "generated1");
    }

    @Test
    void addTrainingReturnsOk() throws Exception {
        var request = new AddTrainingRequest("John.Doe", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 60);

        mockMvc.perform(authenticated(post("/api/v1/trainings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(trainingService).addTraining("John.Doe", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 60);
    }

    @Test
    void addTrainingRejectsNonPositiveDuration() throws Exception {
        var request = new AddTrainingRequest("John.Doe", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 0);

        mockMvc.perform(authenticated(post("/api/v1/trainings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.trainingDuration").exists());
    }

    @Test
    void addTrainingRejectsAnInvalidDateFormat() throws Exception {
        String body = """
                {"traineeUsername":"John.Doe","trainerUsername":"Mary.Smith",
                 "trainingName":"Cardio","trainingDate":"21-07-2026","trainingDuration":60}""";

        mockMvc.perform(authenticated(post("/api/v1/trainings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addTrainingRequiresAuthentication() throws Exception {
        var request = new AddTrainingRequest("John.Doe", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 60);

        mockMvc.perform(post("/api/v1/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addTrainingReturnsNotFoundForUnknownTrainee() throws Exception {
        when(trainingService.addTraining(anyString(), anyString(), anyString(), any(), anyInt()))
                .thenThrow(new NotFoundException("Trainee not found: Ghost"));

        var request = new AddTrainingRequest("Ghost", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 60);

        mockMvc.perform(authenticated(post("/api/v1/trainings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTrainingTypesReturnsTheConstantList() throws Exception {
        TrainingType cardio = new TrainingType();
        cardio.setId(1L);
        cardio.setTrainingTypeName("Cardio");
        when(trainingTypeService.findAll()).thenReturn(List.of(cardio));

        mockMvc.perform(authenticated(get("/api/v1/training-types")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingTypeId").value(1))
                .andExpect(jsonPath("$[0].trainingType").value("Cardio"));
    }

    @Test
    void trainingTypesResponseIsSerializedAsExpected() throws Exception {
        String json = objectMapper.writeValueAsString(new TrainingTypeResponse(1L, "Cardio"));
        org.assertj.core.api.Assertions.assertThat(json)
                .contains("\"trainingTypeId\":1")
                .contains("\"trainingType\":\"Cardio\"");
    }
}
