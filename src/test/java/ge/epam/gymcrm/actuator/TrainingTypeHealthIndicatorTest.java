package ge.epam.gymcrm.actuator;

import ge.epam.gymcrm.dao.TrainingTypeDAO;
import ge.epam.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeHealthIndicatorTest {

    @Mock
    private TrainingTypeDAO trainingTypeDAO;

    @Mock
    private PlatformTransactionManager transactionManager;

    private TrainingTypeHealthIndicator newIndicator() {
        return new TrainingTypeHealthIndicator(trainingTypeDAO, transactionManager);
    }

    @Test
    void reportsUpWhenTrainingTypesAreSeeded() {
        when(trainingTypeDAO.findAll()).thenReturn(List.of(new TrainingType(), new TrainingType()));

        Health health = newIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("count", 2);
    }

    @Test
    void reportsDownWhenNoTrainingTypes() {
        when(trainingTypeDAO.findAll()).thenReturn(List.of());

        Health health = newIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void reportsDownWhenQueryThrows() {
        when(trainingTypeDAO.findAll()).thenThrow(new RuntimeException("db down"));

        Health health = newIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
