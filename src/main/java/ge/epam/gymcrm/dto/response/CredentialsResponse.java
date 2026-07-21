package ge.epam.gymcrm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated credentials returned after registration")
public record CredentialsResponse(

        @Schema(example = "John.Doe")
        String username,

        @Schema(example = "aB3xQ9zL1p")
        String password
) {
}
