package ge.epam.gymcrm.workload;

import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.logging.TransactionContext;
import ge.epam.gymcrm.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkloadNotifier {

    private static final Logger log = LoggerFactory.getLogger(WorkloadNotifier.class);

    private final JmsTemplate jmsTemplate;
    private final JwtService jwtService;
    private final String workloadQueue;
    private final String serviceSubject;

    public WorkloadNotifier(JmsTemplate jmsTemplate,
                            JwtService jwtService,
                            @Value("${gymcrm.messaging.workload-queue}") String workloadQueue,
                            @Value("${spring.application.name:gym-crm-rest}") String serviceSubject) {
        this.jmsTemplate = jmsTemplate;
        this.jwtService = jwtService;
        this.workloadQueue = workloadQueue;
        this.serviceSubject = serviceSubject;
    }

    public void notifyAdded(Training training) {
        publish(training, ActionType.ADD);
    }

    public void notifyDeleted(Training training) {
        publish(training, ActionType.DELETE);
    }

    private void publish(Training training, ActionType actionType) {
        Trainer trainer = training.getTrainer();
        User user = trainer.getUser();
        TrainerWorkloadRequest event = new TrainerWorkloadRequest(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                training.getTrainingDate(),
                training.getTrainingDuration(),
                actionType);

        String transactionId = TransactionContext.currentTransactionId();

        try {
            jmsTemplate.convertAndSend(workloadQueue, event, message -> {
                message.setStringProperty(MessagingHeaders.AUTH_TOKEN,
                        jwtService.generateToken(serviceSubject));
                if (transactionId != null && !transactionId.isBlank()) {
                    message.setStringProperty(MessagingHeaders.TRANSACTION_ID, transactionId);
                }
                return message;
            });
            log.info("Published {} workload event for trainer {} to queue {}",
                    actionType, user.getUsername(), workloadQueue);
        } catch (JmsException ex) {
            log.error("Could not publish {} workload event for trainer {} on {} ({} min): {}",
                    actionType, user.getUsername(), training.getTrainingDate(),
                    training.getTrainingDuration(), ex.getMessage());
        }
    }
}
