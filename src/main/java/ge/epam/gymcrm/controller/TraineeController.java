package ge.epam.gymcrm.controller;

import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.dto.request.ActivationRequest;
import ge.epam.gymcrm.dto.request.TraineeRegistrationRequest;
import ge.epam.gymcrm.dto.request.TraineeUpdateRequest;
import ge.epam.gymcrm.dto.request.UpdateTrainerListRequest;
import ge.epam.gymcrm.dto.response.CredentialsResponse;
import ge.epam.gymcrm.dto.response.ErrorResponse;
import ge.epam.gymcrm.dto.response.TraineeProfileResponse;
import ge.epam.gymcrm.dto.response.TraineeTrainingResponse;
import ge.epam.gymcrm.dto.response.TrainerSummaryResponse;
import ge.epam.gymcrm.mapper.GymMapper;
import ge.epam.gymcrm.service.TraineeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trainees")
@Validated
@Tag(name = "Trainees", description = "Trainee registration, profile and trainings")
public class TraineeController {

    private final TraineeService traineeService;
    private final GymMapper mapper;

    @Autowired
    public TraineeController(TraineeService traineeService, GymMapper mapper) {
        this.traineeService = traineeService;
        this.mapper = mapper;
    }

    @Operation(summary = "Trainee registration",
            description = "Creates a trainee profile and returns the generated credentials. "
                    + "No credentials headers needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainee registered"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already registered as a trainer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<CredentialsResponse> register(
            @Valid @RequestBody TraineeRegistrationRequest request) {

        Trainee trainee = traineeService.register(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address());

        CredentialsResponse body = new CredentialsResponse(
                trainee.getUser().getUsername(), trainee.getUser().getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Get trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee profile"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}")
    public TraineeProfileResponse getProfile(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username) {

        return mapper.toTraineeProfile(traineeService.getByUsername(username));
    }

    @Operation(summary = "Update trainee profile",
            description = "The username identifies the profile and cannot be changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated trainee profile"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{username}")
    public TraineeProfileResponse updateProfile(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username,
            @Valid @RequestBody TraineeUpdateRequest request) {

        Trainee trainee = traineeService.update(username, request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address(), request.isActive());
        return mapper.toTraineeProfile(trainee);
    }

    @Operation(summary = "Delete trainee profile",
            description = "Hard delete — the trainee's trainings are removed as well.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteProfile(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username) {

        traineeService.delete(username);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get active trainers not assigned to the trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unassigned active trainers"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}/unassigned-trainers")
    public List<TrainerSummaryResponse> getUnassignedTrainers(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username) {

        return mapper.toTrainerSummaries(traineeService.getNotAssignedActiveTrainers(username));
    }

    @Operation(summary = "Update the trainee's trainer list",
            description = "Replaces the whole list with the given trainers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated trainer list"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee or trainer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{username}/trainers")
    public List<TrainerSummaryResponse> updateTrainers(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username,
            @Valid @RequestBody UpdateTrainerListRequest request) {

        return mapper.toTrainerSummaries(
                traineeService.updateTrainers(username, request.trainerUsernames()));
    }

    @Operation(summary = "Get trainee trainings list",
            description = "Optional filters: period, trainer name and training type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching trainings"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}/trainings")
    public List<TraineeTrainingResponse> getTrainings(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username,

            @Parameter(description = "Period from (inclusive)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,

            @Parameter(description = "Period to (inclusive)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,

            @Parameter(description = "Trainer name or username", example = "Mary")
            @RequestParam(required = false) String trainerName,

            @Parameter(description = "Training type name", example = "Cardio")
            @RequestParam(required = false) String trainingType) {

        return traineeService.getTrainings(username, periodFrom, periodTo, trainerName, trainingType)
                .stream()
                .map(mapper::toTraineeTraining)
                .toList();
    }

    @Operation(summary = "Activate / de-activate trainee",
            description = "Not idempotent: setting the state the trainee is already in returns 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "State changed", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Trainee is already in that state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainee username", example = "John.Doe")
            @PathVariable String username,
            @Valid @RequestBody ActivationRequest request) {

        traineeService.setActive(username, request.isActive());
        return ResponseEntity.ok().build();
    }
}
