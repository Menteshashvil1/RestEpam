package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TrainingDAO;
import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.NotFoundException;
import ge.epam.gymcrm.metrics.GymMetrics;
import ge.epam.gymcrm.workload.WorkloadNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDAO trainingDAO;

    @Mock
    private TraineeService traineeService;

    @Mock
    private TrainerService trainerService;

    @Mock
    private GymMetrics metrics;

    @Mock
    private WorkloadNotifier workloadNotifier;

    @InjectMocks
    private TrainingService trainingService;

    private Trainee trainee;
    private Trainer trainer;
    private TrainingType cardio;

    @BeforeEach
    void setUp() {
        cardio = new TrainingType();
        cardio.setId(1L);
        cardio.setTrainingTypeName("Cardio");

        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setUser(user("John", "Doe", "John.Doe"));

        trainer = new Trainer();
        trainer.setId(2L);
        trainer.setUser(user("Mary", "Smith", "Mary.Smith"));
        trainer.setSpecialization(cardio);
    }

    private User user(String firstName, String lastName, String username) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setActive(true);
        return user;
    }

    @Test
    void addTrainingTakesTheTypeFromTheTrainerSpecialization() {
        when(traineeService.getByUsername("John.Doe")).thenReturn(trainee);
        when(trainerService.getByUsername("Mary.Smith")).thenReturn(trainer);

        Training training = trainingService.addTraining(
                "John.Doe", "Mary.Smith", "Morning cardio", LocalDate.of(2026, 7, 21), 60);

        assertThat(training.getTrainingType()).isEqualTo(cardio);
        assertThat(training.getTrainingDuration()).isEqualTo(60);

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingDAO).save(captor.capture());
        assertThat(captor.getValue().getTrainingName()).isEqualTo("Morning cardio");
    }

    @Test
    void addTrainingAssignsTheTrainerToTheTrainee() {
        when(traineeService.getByUsername("John.Doe")).thenReturn(trainee);
        when(trainerService.getByUsername("Mary.Smith")).thenReturn(trainer);

        trainingService.addTraining("John.Doe", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 60);

        assertThat(trainee.getTrainers()).containsExactly(trainer);
    }

    @Test
    void addTrainingNotifiesTheWorkloadService() {
        when(traineeService.getByUsername("John.Doe")).thenReturn(trainee);
        when(trainerService.getByUsername("Mary.Smith")).thenReturn(trainer);

        Training training = trainingService.addTraining("John.Doe", "Mary.Smith", "Morning cardio",
                LocalDate.of(2026, 7, 21), 60);

        verify(workloadNotifier).notifyAdded(training);
    }

    @Test
    void deleteTrainingNotifiesTheWorkloadServiceAndRemovesTheTraining() {
        Training training = new Training();
        training.setId(5L);
        training.setTrainer(trainer);
        training.setTrainingName("Morning cardio");
        training.setTrainingDate(LocalDate.of(2026, 7, 21));
        training.setTrainingDuration(60);
        when(trainingDAO.findById(5L)).thenReturn(java.util.Optional.of(training));

        trainingService.deleteTraining(5L);

        verify(workloadNotifier).notifyDeleted(training);
        verify(trainingDAO).delete(training);
    }

    @Test
    void deleteTrainingFailsWhenTheTrainingDoesNotExist() {
        when(trainingDAO.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> trainingService.deleteTraining(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addTrainingFailsWhenTheTraineeDoesNotExist() {
        when(traineeService.getByUsername("Ghost")).thenThrow(new NotFoundException("Trainee not found: Ghost"));

        assertThatThrownBy(() -> trainingService.addTraining("Ghost", "Mary.Smith", "Cardio",
                LocalDate.of(2026, 7, 21), 60))
                .isInstanceOf(NotFoundException.class);
    }
}
