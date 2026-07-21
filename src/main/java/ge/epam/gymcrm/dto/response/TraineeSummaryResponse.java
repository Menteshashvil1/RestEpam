package ge.epam.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short trainee view used inside trainer profiles")
public record TraineeSummaryResponse(

        @Schema(example = "John.Doe")
        String username,

        @Schema(example = "John")
        String firstName,

        @Schema(example = "Doe")
        String lastName
) {
}
