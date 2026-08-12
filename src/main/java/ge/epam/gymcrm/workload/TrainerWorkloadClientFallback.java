package ge.epam.gymcrm.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Circuit-breaker fallback. When the workload service is unreachable (or the breaker is open),
 * the event is logged instead of propagating the failure, so adding/deleting a training still
 * succeeds locally.
 */
@Component
public class TrainerWorkloadClientFallback implements FallbackFactory<TrainerWorkloadClient> {

    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadClientFallback.class);

    @Override
    public TrainerWorkloadClient create(Throwable cause) {
        return request -> log.warn(
                "Workload service unavailable ({}). Dropped {} event for trainer {} on {} ({} min).",
                cause.toString(), request.actionType(), request.trainerUsername(),
                request.trainingDate(), request.trainingDuration());
    }
}
