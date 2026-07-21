package ge.epam.gymcrm.controller;

import ge.epam.gymcrm.dto.response.ErrorResponse;
import ge.epam.gymcrm.dto.response.TrainingTypeResponse;
import ge.epam.gymcrm.mapper.GymMapper;
import ge.epam.gymcrm.service.TrainingTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/training-types")
@Tag(name = "Training types", description = "Read only reference list of training types")
public class TrainingTypeController {

    private final TrainingTypeService trainingTypeService;
    private final GymMapper mapper;

    @Autowired
    public TrainingTypeController(TrainingTypeService trainingTypeService, GymMapper mapper) {
        this.trainingTypeService = trainingTypeService;
        this.mapper = mapper;
    }

    @Operation(summary = "Get training types",
            description = "Returns the constant list of training types. It cannot be modified from the application.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training types"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<TrainingTypeResponse> getTrainingTypes() {
        return trainingTypeService.findAll().stream()
                .map(mapper::toTrainingType)
                .toList();
    }
}
