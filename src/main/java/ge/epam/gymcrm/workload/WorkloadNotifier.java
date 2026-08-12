package ge.epam.gymcrm.workload;

import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Builds workload events from domain objects and forwards them to the secondary microservice. */
@Component
public class WorkloadNotifier {

    private static final Logger log = LoggerFactory.getLogger(WorkloadNotifier.class);

    private final TrainerWorkloadClient client;

    public WorkloadNotifier(TrainerWorkloadClient client) {
        this.client = client;
    }

    public void notifyAdded(Training training) {
        send(training.getTrainer(), training, ActionType.ADD);
    }

    public void notifyDeleted(Training training) {
        send(training.getTrainer(), training, ActionType.DELETE);
    }

    private void send(Trainer trainer, Training training, ActionType actionType) {
        User user = trainer.getUser();
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                training.getTrainingDate(),
                training.getTrainingDuration(),
                actionType);

        log.info("Notifying workload service: {} for trainer {}", actionType, user.getUsername());
        client.sendWorkload(request);
    }
}
