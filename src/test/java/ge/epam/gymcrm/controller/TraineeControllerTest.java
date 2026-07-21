package ge.epam.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.config.AuthenticationInterceptor;
import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.dto.request.ActivationRequest;
import ge.epam.gymcrm.dto.request.TraineeRegistrationRequest;
import ge.epam.gymcrm.dto.request.TraineeUpdateRequest;
import ge.epam.gymcrm.dto.request.UpdateTrainerListRequest;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.exception.ConflictException;
import ge.epam.gymcrm.exception.NotFoundException;
import ge.epam.gymcrm.mapper.GymMapper;
import ge.epam.gymcrm.service.TraineeService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TraineeController.class)
@Import({AuthenticationInterceptor.class, GymMapper.class})
class TraineeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TraineeService traineeService;

    @MockBean
    private UserService userService;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername("John.Doe");
        user.setPassword("generated1");
        user.setActive(true);

        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setUser(user);
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 23));
        trainee.setAddress("Tbilisi");

        when(userService.authenticate(anyString(), anyString())).thenReturn(user);
    }

    /** Adds the credentials headers every authenticated endpoint requires. */
    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder builder) {
        return builder
                .header(AuthenticationInterceptor.USERNAME_HEADER, "John.Doe")
                .header(AuthenticationInterceptor.PASSWORD_HEADER, "generated1");
    }

    @Test
    void registerReturnsGeneratedCredentials() throws Exception {
        when(traineeService.register(eq("John"), eq("Doe"), any(), any())).thenReturn(trainee);

        var request = new TraineeRegistrationRequest("John", "Doe", LocalDate.of(1995, 4, 23), "Tbilisi");

        mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John.Doe"))
                .andExpect(jsonPath("$.password").value("generated1"));
    }

    @Test
    void registerRejectsBlankFirstName() throws Exception {
        var request = new TraineeRegistrationRequest("", "Doe", null, null);

        mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.firstName").exists());
    }

    @Test
    void registerReturnsConflictWhenAlreadyATrainer() throws Exception {
        when(traineeService.register(any(), any(), any(), any()))
                .thenThrow(new ConflictException("already registered as a trainer"));

        var request = new TraineeRegistrationRequest("John", "Doe", null, null);

        mockMvc.perform(post("/api/v1/trainees/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/trainees/John.Doe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileFailsWithWrongCredentials() throws Exception {
        when(userService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationFailedException("Invalid username or password"));

        mockMvc.perform(authenticated(get("/api/v1/trainees/John.Doe")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileReturnsTheProfileWithTrainers() throws Exception {
        trainee.setTrainers(List.of(trainerNamed("Mary.Smith")));
        when(traineeService.getByUsername("John.Doe")).thenReturn(trainee);

        mockMvc.perform(authenticated(get("/api/v1/trainees/John.Doe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.address").value("Tbilisi"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers[0].username").value("Mary.Smith"));
    }

    @Test
    void getProfileReturnsNotFoundForUnknownTrainee() throws Exception {
        when(traineeService.getByUsername("Ghost")).thenThrow(new NotFoundException("Trainee not found: Ghost"));

        mockMvc.perform(authenticated(get("/api/v1/trainees/Ghost")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateProfileReturnsTheUpdatedProfile() throws Exception {
        when(traineeService.update(eq("John.Doe"), eq("John"), eq("Doe"), any(), any(), eq(true)))
                .thenReturn(trainee);

        var request = new TraineeUpdateRequest("John", "Doe", LocalDate.of(1995, 4, 23), "Tbilisi", true);

        mockMvc.perform(authenticated(put("/api/v1/trainees/John.Doe"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Doe"));
    }

    @Test
    void updateProfileRejectsMissingIsActive() throws Exception {
        String body = """
                {"firstName":"John","lastName":"Doe"}""";

        mockMvc.perform(authenticated(put("/api/v1/trainees/John.Doe"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.isActive").exists());
    }

    @Test
    void deleteProfileReturnsOk() throws Exception {
        mockMvc.perform(authenticated(delete("/api/v1/trainees/John.Doe")))
                .andExpect(status().isOk());

        verify(traineeService).delete("John.Doe");
    }

    @Test
    void getUnassignedTrainersReturnsTheList() throws Exception {
        when(traineeService.getNotAssignedActiveTrainers("John.Doe"))
                .thenReturn(List.of(trainerNamed("Mary.Smith")));

        mockMvc.perform(authenticated(get("/api/v1/trainees/John.Doe/unassigned-trainers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Mary.Smith"))
                .andExpect(jsonPath("$[0].specialization.trainingType").value("Cardio"));
    }

    @Test
    void updateTrainersRejectsAnEmptyList() throws Exception {
        var request = new UpdateTrainerListRequest(List.of());

        mockMvc.perform(authenticated(put("/api/v1/trainees/John.Doe/trainers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTrainersReturnsTheNewList() throws Exception {
        when(traineeService.updateTrainers("John.Doe", List.of("Mary.Smith")))
                .thenReturn(List.of(trainerNamed("Mary.Smith")));

        var request = new UpdateTrainerListRequest(List.of("Mary.Smith"));

        mockMvc.perform(authenticated(put("/api/v1/trainees/John.Doe/trainers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Mary"));
    }

    @Test
    void setActiveReturnsConflictWhenAlreadyInThatState() throws Exception {
        doThrow(new ConflictException("Trainee John.Doe is already active"))
                .when(traineeService).setActive(anyString(), anyBoolean());

        mockMvc.perform(authenticated(patch("/api/v1/trainees/John.Doe/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivationRequest(true))))
                .andExpect(status().isConflict());
    }

    @Test
    void setActiveReturnsOk() throws Exception {
        mockMvc.perform(authenticated(patch("/api/v1/trainees/John.Doe/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivationRequest(false))))
                .andExpect(status().isOk());

        verify(traineeService).setActive("John.Doe", false);
    }

    private Trainer trainerNamed(String username) {
        User user = new User();
        user.setFirstName("Mary");
        user.setLastName("Smith");
        user.setUsername(username);
        user.setActive(true);

        var type = new ge.epam.gymcrm.domain.TrainingType();
        type.setId(1L);
        type.setTrainingTypeName("Cardio");

        Trainer trainer = new Trainer();
        trainer.setId(2L);
        trainer.setUser(user);
        trainer.setSpecialization(type);
        return trainer;
    }
}
