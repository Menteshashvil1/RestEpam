package ge.epam.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Activate / de-activate request. The action is not idempotent: "
        + "requesting the state a profile is already in returns 409 Conflict.")
public record ActivationRequest(

        @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isActive is required")
        @JsonProperty("isActive")
        Boolean isActive
) {
}
