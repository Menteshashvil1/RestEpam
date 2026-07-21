package ge.epam.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short trainer view used inside trainee profiles and trainer lists")
public record TrainerSummaryResponse(

        @Schema(example = "Mary.Smith")
        String username,

        @Schema(example = "Mary")
        String firstName,

        @Schema(example = "Smith")
        String lastName,

        TrainingTypeResponse specialization
) {
}
