package ge.epam.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Add training request")
public record AddTrainingRequest(

        @Schema(example = "John.Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Trainee username is required")
        String traineeUsername,

        @Schema(example = "Mary.Smith", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Trainer username is required")
        String trainerUsername,

        @Schema(example = "Morning cardio", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Training name is required")
        @Size(max = 100, message = "Training name must not exceed 100 characters")
        String trainingName,

        @Schema(example = "2026-07-21", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Training date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate trainingDate,

        @Schema(description = "Training duration in minutes", example = "60",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Training duration is required")
        @Positive(message = "Training duration must be a positive number")
        @Max(value = 600, message = "Training duration must not exceed 600 minutes")
        Integer trainingDuration
) {
}
