package ge.epam.gymcrm.workload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** Payload sent to the secondary (Trainer Workload) microservice. */
public record TrainerWorkloadRequest(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        boolean isActive,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate trainingDate,
        int trainingDuration,
        ActionType actionType
) {
}
