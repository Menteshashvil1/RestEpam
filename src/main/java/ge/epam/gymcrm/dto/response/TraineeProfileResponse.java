package ge.epam.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Trainee profile")
public record TraineeProfileResponse(

        @Schema(example = "John.Doe")
        String username,

        @Schema(example = "John")
        String firstName,

        @Schema(example = "Doe")
        String lastName,

        @Schema(example = "1995-04-23")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @Schema(example = "12 Rustaveli Ave, Tbilisi")
        String address,

        @Schema(example = "true")
        @JsonProperty("isActive")
        boolean isActive,

        @Schema(description = "Trainers assigned to this trainee")
        List<TrainerSummaryResponse> trainers
) {
}
