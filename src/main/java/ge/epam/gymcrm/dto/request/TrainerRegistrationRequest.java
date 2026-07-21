package ge.epam.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Trainer registration request")
public record TrainerRegistrationRequest(

        @Schema(example = "Mary", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Schema(example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Schema(description = "Training type id the trainer specializes in", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Specialization is required")
        Long specializationId
) {
}
