package ge.epam.gymcrm.workload;

import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkloadNotifierTest {

    private static final String QUEUE = "trainer.workload.queue";

    @Mock
    private JmsTemplate jmsTemplate;

    private WorkloadNotifier notifier;
    private Training training;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(
                "test-secret-test-secret-test-secret-0123456789-abcdef", 60);
        notifier = new WorkloadNotifier(jmsTemplate, jwtService, QUEUE, "gym-crm-rest");

        User user = new User();
        user.setFirstName("Mary");
        user.setLastName("Smith");
        user.setUsername("Mary.Smith");
        user.setActive(true);

        Trainer trainer = new Trainer();
        trainer.setUser(user);

        training = new Training();
        training.setTrainer(trainer);
        training.setTrainingName("Morning cardio");
        training.setTrainingDate(LocalDate.of(2026, 7, 21));
        training.setTrainingDuration(60);
    }

    private TrainerWorkloadRequest publishedEvent() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(jmsTemplate).convertAndSend(eq(QUEUE), payload.capture(), any(MessagePostProcessor.class));
        assertThat(payload.getValue()).isInstanceOf(TrainerWorkloadRequest.class);
        return (TrainerWorkloadRequest) payload.getValue();
    }

    @Test
    void publishesAnAddEventForAnAddedTraining() {
        notifier.notifyAdded(training);

        TrainerWorkloadRequest event = publishedEvent();
        assertThat(event.actionType()).isEqualTo(ActionType.ADD);
        assertThat(event.trainerUsername()).isEqualTo("Mary.Smith");
        assertThat(event.trainerFirstName()).isEqualTo("Mary");
        assertThat(event.trainerLastName()).isEqualTo("Smith");
        assertThat(event.isActive()).isTrue();
        assertThat(event.trainingDate()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(event.trainingDuration()).isEqualTo(60);
    }

    @Test
    void publishesADeleteEventForARemovedTraining() {
        notifier.notifyDeleted(training);

        assertThat(publishedEvent().actionType()).isEqualTo(ActionType.DELETE);
    }

    @Test
    void carriesTheTrainerStatusOfAnInactiveTrainer() {
        training.getTrainer().getUser().setActive(false);

        notifier.notifyAdded(training);

        assertThat(publishedEvent().isActive()).isFalse();
    }

    @Test
    void anUnreachableBrokerDoesNotBreakTheTrainingFlow() {
        doThrow(new UncategorizedJmsException("broker down"))
                .when(jmsTemplate).convertAndSend(eq(QUEUE), any(), any(MessagePostProcessor.class));

        assertThatCode(() -> notifier.notifyAdded(training)).doesNotThrowAnyException();
    }
}
