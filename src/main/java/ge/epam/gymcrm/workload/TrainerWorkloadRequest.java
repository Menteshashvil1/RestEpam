package ge.epam.gymcrm.workload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TrainerWorkloadRequest(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        @JsonProperty("isActive") boolean isActive,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate trainingDate,
        int trainingDuration,
        ActionType actionType
) {
}
