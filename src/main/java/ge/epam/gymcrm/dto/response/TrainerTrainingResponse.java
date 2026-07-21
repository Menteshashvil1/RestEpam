package ge.epam.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Training as seen from the trainer's side")
public record TrainerTrainingResponse(

        @Schema(example = "Morning cardio")
        String trainingName,

        @Schema(example = "2026-07-21")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate trainingDate,

        @Schema(example = "Cardio")
        String trainingType,

        @Schema(description = "Duration in minutes", example = "60")
        int trainingDuration,

        @Schema(example = "John Doe")
        String traineeName
) {
}
