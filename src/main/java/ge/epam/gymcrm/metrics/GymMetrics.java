package ge.epam.gymcrm.metrics;

import ge.epam.gymcrm.dao.TraineeDAO;
import ge.epam.gymcrm.dao.TrainerDAO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class GymMetrics {

    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter trainingsCreated;
    private final Counter authSuccesses;
    private final Counter authFailures;

    public GymMetrics(MeterRegistry registry,
                      TraineeDAO traineeDAO,
                      TrainerDAO trainerDAO,
                      PlatformTransactionManager transactionManager) {

        this.traineeRegistrations = Counter.builder("gym.trainee.registrations")
                .description("Total number of trainee profiles registered")
                .register(registry);
        this.trainerRegistrations = Counter.builder("gym.trainer.registrations")
                .description("Total number of trainer profiles registered")
                .register(registry);
        this.trainingsCreated = Counter.builder("gym.trainings.created")
                .description("Total number of trainings created")
                .register(registry);
        this.authSuccesses = Counter.builder("gym.authentication.attempts")
                .tag("result", "success")
                .description("Total number of authentication attempts")
                .register(registry);
        this.authFailures = Counter.builder("gym.authentication.attempts")
                .tag("result", "failure")
                .description("Total number of authentication attempts")
                .register(registry);

        TransactionTemplate readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        Gauge.builder("gym.trainees.count",
                        () -> readOnlyTx.execute(status -> traineeDAO.count()))
                .description("Current number of trainee profiles")
                .register(registry);
        Gauge.builder("gym.trainers.count",
                        () -> readOnlyTx.execute(status -> trainerDAO.count()))
                .description("Current number of trainer profiles")
                .register(registry);
    }

    public void recordTraineeRegistration() {
        traineeRegistrations.increment();
    }

    public void recordTrainerRegistration() {
        trainerRegistrations.increment();
    }

    public void recordTrainingCreated() {
        trainingsCreated.increment();
    }

    public void recordAuthenticationSuccess() {
        authSuccesses.increment();
    }

    public void recordAuthenticationFailure() {
        authFailures.increment();
    }
}
