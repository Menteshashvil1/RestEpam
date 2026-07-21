package ge.epam.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Training type reference")
public record TrainingTypeResponse(

        @Schema(example = "1")
        Long trainingTypeId,

        @Schema(example = "Cardio")
        String trainingType
) {
}
