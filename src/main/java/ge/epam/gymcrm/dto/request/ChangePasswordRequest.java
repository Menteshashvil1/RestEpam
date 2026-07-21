package ge.epam.gymcrm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Change login (password) request")
public record ChangePasswordRequest(

        @Schema(example = "John.Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is required")
        String username,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Old password is required")
        String oldPassword,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 30, message = "New password must be between 8 and 30 characters")
        String newPassword
) {
}
