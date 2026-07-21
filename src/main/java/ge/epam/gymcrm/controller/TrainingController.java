package ge.epam.gymcrm.controller;

import ge.epam.gymcrm.dto.request.AddTrainingRequest;
import ge.epam.gymcrm.dto.response.ErrorResponse;
import ge.epam.gymcrm.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainings")
@Tag(name = "Trainings", description = "Adding trainings. Trainings cannot be updated or deleted via REST.")
public class TrainingController {

    private final TrainingService trainingService;

    @Autowired
    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Operation(summary = "Add training",
            description = "Creates a training between the given trainee and trainer. "
                    + "The training type is taken from the trainer's specialization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training added", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainee or trainer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Void> addTraining(@Valid @RequestBody AddTrainingRequest request) {
        trainingService.addTraining(
                request.traineeUsername(),
                request.trainerUsername(),
                request.trainingName(),
                request.trainingDate(),
                request.trainingDuration());
        return ResponseEntity.ok().build();
    }
}
