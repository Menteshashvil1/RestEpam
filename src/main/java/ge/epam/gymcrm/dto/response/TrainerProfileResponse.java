package ge.epam.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Trainer profile")
public record TrainerProfileResponse(

        @Schema(example = "Mary.Smith")
        String username,

        @Schema(example = "Mary")
        String firstName,

        @Schema(example = "Smith")
        String lastName,

        TrainingTypeResponse specialization,

        @Schema(example = "true")
        @JsonProperty("isActive")
        boolean isActive,

        @Schema(description = "Trainees assigned to this trainer")
        List<TraineeSummaryResponse> trainees
) {
}
