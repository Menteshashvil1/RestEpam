package ge.epam.gymcrm.metrics;

import ge.epam.gymcrm.dao.TraineeDAO;
import ge.epam.gymcrm.dao.TrainerDAO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GymMetricsTest {

    @Mock
    private TraineeDAO traineeDAO;

    @Mock
    private TrainerDAO trainerDAO;

    @Mock
    private PlatformTransactionManager transactionManager;

    private SimpleMeterRegistry registry;
    private GymMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new GymMetrics(registry, traineeDAO, trainerDAO, transactionManager);
    }

    @Test
    void registrationCountersIncrement() {
        metrics.recordTraineeRegistration();
        metrics.recordTraineeRegistration();
        metrics.recordTrainerRegistration();

        assertThat(registry.get("gym.trainee.registrations").counter().count()).isEqualTo(2);
        assertThat(registry.get("gym.trainer.registrations").counter().count()).isEqualTo(1);
    }

    @Test
    void trainingCounterIncrements() {
        metrics.recordTrainingCreated();

        assertThat(registry.get("gym.trainings.created").counter().count()).isEqualTo(1);
    }

    @Test
    void authenticationCountersAreTaggedByResult() {
        metrics.recordAuthenticationSuccess();
        metrics.recordAuthenticationFailure();
        metrics.recordAuthenticationFailure();

        assertThat(registry.get("gym.authentication.attempts").tag("result", "success").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("gym.authentication.attempts").tag("result", "failure").counter().count())
                .isEqualTo(2);
    }

    @Test
    void gaugesReportCurrentCounts() {
        lenient().when(traineeDAO.count()).thenReturn(7L);
        lenient().when(trainerDAO.count()).thenReturn(3L);

        assertThat(registry.get("gym.trainees.count").gauge().value()).isEqualTo(7.0);
        assertThat(registry.get("gym.trainers.count").gauge().value()).isEqualTo(3.0);
    }
}
