package ge.epam.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = """
        Update trainer profile request. The username cannot be changed and the
        specialization is read only — it is returned in the response but never modified.""")
public record TrainerUpdateRequest(

        @Schema(example = "Mary", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Schema(example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isActive is required")
        @JsonProperty("isActive")
        Boolean isActive
) {
}
