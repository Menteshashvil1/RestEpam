package ge.epam.gymcrm.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Update trainee profile request. The username cannot be changed.")
public record TraineeUpdateRequest(

        @Schema(example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Schema(example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Schema(example = "1995-04-23")
        @Past(message = "Date of birth must be in the past")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @Schema(example = "12 Rustaveli Ave, Tbilisi")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "isActive is required")
        @JsonProperty("isActive")
        Boolean isActive
) {
}
