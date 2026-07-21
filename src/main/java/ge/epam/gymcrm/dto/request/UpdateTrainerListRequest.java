package ge.epam.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Replaces the trainee's trainer list")
public record UpdateTrainerListRequest(

        @Schema(description = "Usernames of the trainers to assign",
                example = "[\"Mary.Smith\", \"Nick.Jones\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Trainers list is required")
        List<@NotEmpty(message = "Trainer username must not be blank") String> trainerUsernames
) {
}
