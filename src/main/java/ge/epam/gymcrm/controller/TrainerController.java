package ge.epam.gymcrm.controller;

import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.dto.request.ActivationRequest;
import ge.epam.gymcrm.dto.request.TrainerRegistrationRequest;
import ge.epam.gymcrm.dto.request.TrainerUpdateRequest;
import ge.epam.gymcrm.dto.response.CredentialsResponse;
import ge.epam.gymcrm.dto.response.ErrorResponse;
import ge.epam.gymcrm.dto.response.TrainerProfileResponse;
import ge.epam.gymcrm.dto.response.TrainerTrainingResponse;
import ge.epam.gymcrm.mapper.GymMapper;
import ge.epam.gymcrm.service.AuthService;
import ge.epam.gymcrm.service.TrainerService;
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
@RequestMapping("/api/v1/trainers")
@Validated
@Tag(name = "Trainers", description = "Trainer registration, profile and trainings")
public class TrainerController {

    private final TrainerService trainerService;
    private final AuthService authService;
    private final GymMapper mapper;

    @Autowired
    public TrainerController(TrainerService trainerService, AuthService authService, GymMapper mapper) {
        this.trainerService = trainerService;
        this.authService = authService;
        this.mapper = mapper;
    }

    @Operation(summary = "Trainer registration",
            description = "Creates a trainer profile and returns the generated credentials. "
                    + "No credentials headers needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer registered"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unknown specialization",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already registered as a trainee",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<CredentialsResponse> register(
            @Valid @RequestBody TrainerRegistrationRequest request) {

        Trainer trainer = trainerService.register(
                request.firstName(), request.lastName(), request.specializationId());

        String username = trainer.getUser().getUsername();
        CredentialsResponse body = new CredentialsResponse(
                username, trainer.getUser().getRawPassword(), authService.issueToken(username));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Get trainer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer profile"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}")
    public TrainerProfileResponse getProfile(
            @Parameter(description = "Trainer username", example = "Mary.Smith")
            @PathVariable String username) {

        return mapper.toTrainerProfile(trainerService.getByUsername(username));
    }

    @Operation(summary = "Update trainer profile",
            description = "The username identifies the profile and cannot be changed; "
                    + "the specialization is read only and is only echoed back in the response.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated trainer profile"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{username}")
    public TrainerProfileResponse updateProfile(
            @Parameter(description = "Trainer username", example = "Mary.Smith")
            @PathVariable String username,
            @Valid @RequestBody TrainerUpdateRequest request) {

        Trainer trainer = trainerService.update(
                username, request.firstName(), request.lastName(), request.isActive());
        return mapper.toTrainerProfile(trainer);
    }

    @Operation(summary = "Get trainer trainings list",
            description = "Optional filters: period and trainee name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching trainings"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}/trainings")
    public List<TrainerTrainingResponse> getTrainings(
            @Parameter(description = "Trainer username", example = "Mary.Smith")
            @PathVariable String username,

            @Parameter(description = "Period from (inclusive)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,

            @Parameter(description = "Period to (inclusive)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,

            @Parameter(description = "Trainee name or username", example = "John")
            @RequestParam(required = false) String traineeName) {

        return trainerService.getTrainings(username, periodFrom, periodTo, traineeName)
                .stream()
                .map(mapper::toTrainerTraining)
                .toList();
    }

    @Operation(summary = "Activate / de-activate trainer",
            description = "Not idempotent: setting the state the trainer is already in returns 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "State changed", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Trainer is already in that state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainer username", example = "Mary.Smith")
            @PathVariable String username,
            @Valid @RequestBody ActivationRequest request) {

        trainerService.setActive(username, request.isActive());
        return ResponseEntity.ok().build();
    }
}
