package ge.epam.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bearer token returned by login")
public record TokenResponse(

        @Schema(example = "John.Doe")
        String username,

        @Schema(description = "JWT to send as 'Authorization: Bearer <token>'")
        String token
) {
}
