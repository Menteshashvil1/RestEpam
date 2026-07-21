package ge.epam.gymcrm.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Standard error payload returned by every endpoint")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        LocalDateTime timestamp,

        @Schema(example = "400")
        int status,

        @Schema(example = "Bad Request")
        String error,

        @Schema(example = "Validation failed")
        String message,

        @Schema(example = "/api/v1/trainees")
        String path,

        @Schema(description = "Id of the transaction this call belongs to — use it to trace the logs",
                example = "8f1b2f0c-2a7e-4c3e-9a4f-2e1b6d9c0f11")
        String transactionId,

        @Schema(description = "Field level validation errors, present only for validation failures")
        Map<String, String> validationErrors
) {
}
