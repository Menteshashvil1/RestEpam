package ge.epam.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.dto.request.ActivationRequest;
import ge.epam.gymcrm.dto.request.TrainerRegistrationRequest;
import ge.epam.gymcrm.dto.request.TrainerUpdateRequest;
import ge.epam.gymcrm.exception.NotFoundException;
import ge.epam.gymcrm.mapper.GymMapper;
import ge.epam.gymcrm.security.JwtService;
import ge.epam.gymcrm.security.TokenBlacklistService;
import ge.epam.gymcrm.service.AuthService;
import ge.epam.gymcrm.service.TrainerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GymMapper.class)
class TrainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainerService trainerService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private UserDetailsService userDetailsService;

    private Trainer trainer;
    private TrainingType cardio;

    @BeforeEach
    void setUp() {
        cardio = new TrainingType();
        cardio.setId(1L);
        cardio.setTrainingTypeName("Cardio");

        User user = new User();
        user.setFirstName("Mary");
        user.setLastName("Smith");
        user.setUsername("Mary.Smith");
        user.setPassword("$2a$hashed");
        user.setRawPassword("generated1");
        user.setActive(true);

        trainer = new Trainer();
        trainer.setId(1L);
        trainer.setUser(user);
        trainer.setSpecialization(cardio);

        when(authService.issueToken(anyString())).thenReturn("jwt-token");
    }

    @Test
    void registerReturnsGeneratedCredentialsAndToken() throws Exception {
        when(trainerService.register("Mary", "Smith", 1L)).thenReturn(trainer);

        var request = new TrainerRegistrationRequest("Mary", "Smith", 1L);

        mockMvc.perform(post("/api/v1/trainers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Mary.Smith"))
                .andExpect(jsonPath("$.password").value("generated1"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void registerRejectsMissingSpecialization() throws Exception {
        var request = new TrainerRegistrationRequest("Mary", "Smith", null);

        mockMvc.perform(post("/api/v1/trainers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.specializationId").exists());
    }

    @Test
    void registerReturnsNotFoundForUnknownSpecialization() throws Exception {
        when(trainerService.register(any(), any(), any()))
                .thenThrow(new NotFoundException("Training type not found: 99"));

        var request = new TrainerRegistrationRequest("Mary", "Smith", 99L);

        mockMvc.perform(post("/api/v1/trainers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfileReturnsTheProfileWithTrainees() throws Exception {
        trainer.setTrainees(List.of(traineeNamed("John.Doe")));
        when(trainerService.getByUsername("Mary.Smith")).thenReturn(trainer);

        mockMvc.perform(get("/api/v1/trainers/Mary.Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Mary"))
                .andExpect(jsonPath("$.specialization.trainingType").value("Cardio"))
                .andExpect(jsonPath("$.specialization.trainingTypeId").value(1))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainees[0].username").value("John.Doe"));
    }

    @Test
    void updateProfileIgnoresSpecializationAndReturnsIt() throws Exception {
        when(trainerService.update("Mary.Smith", "Marianne", "Smith", true)).thenReturn(trainer);

        var request = new TrainerUpdateRequest("Marianne", "Smith", true);

        mockMvc.perform(put("/api/v1/trainers/Mary.Smith")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Mary.Smith"))
                .andExpect(jsonPath("$.specialization.trainingType").value("Cardio"));
    }

    @Test
    void getTrainingsAppliesTheFilters() throws Exception {
        when(trainerService.getTrainings(eq("Mary.Smith"), eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 12, 31)), eq("John")))
                .thenReturn(List.of(training()));

        mockMvc.perform(get("/api/v1/trainers/Mary.Smith/trainings")
                        .param("periodFrom", "2026-01-01")
                        .param("periodTo", "2026-12-31")
                        .param("traineeName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning cardio"))
                .andExpect(jsonPath("$[0].trainingType").value("Cardio"))
                .andExpect(jsonPath("$[0].trainingDuration").value(60))
                .andExpect(jsonPath("$[0].traineeName").value("John Doe"));
    }

    @Test
    void setActiveReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/v1/trainers/Mary.Smith/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivationRequest(false))))
                .andExpect(status().isOk());

        verify(trainerService).setActive("Mary.Smith", false);
    }

    private Trainee traineeNamed(String username) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername(username);
        user.setActive(true);

        Trainee trainee = new Trainee();
        trainee.setId(3L);
        trainee.setUser(user);
        return trainee;
    }

    private Training training() {
        Training training = new Training();
        training.setTrainingName("Morning cardio");
        training.setTrainingDate(LocalDate.of(2026, 7, 21));
        training.setTrainingDuration(60);
        training.setTrainingType(cardio);
        training.setTrainer(trainer);
        training.setTrainee(traineeNamed("John.Doe"));
        return training;
    }
}
