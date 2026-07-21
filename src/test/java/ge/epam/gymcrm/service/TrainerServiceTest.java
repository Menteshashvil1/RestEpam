package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TraineeDAO;
import ge.epam.gymcrm.dao.TrainerDAO;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.ConflictException;
import ge.epam.gymcrm.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock
    private TrainerDAO trainerDAO;

    @Mock
    private TraineeDAO traineeDAO;

    @Mock
    private TrainingTypeService trainingTypeService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TrainerService trainerService;

    private Trainer trainer;
    private TrainingType cardio;

    @BeforeEach
    void setUp() {
        cardio = new TrainingType();
        cardio.setId(1L);
        cardio.setTrainingTypeName("Cardio");

        User user = new User();
        user.setFirstName("Mary");
        user.setLastName("Smith");
        user.setUsername("Mary.Smith");
        user.setPassword("secret1234");
        user.setActive(true);

        trainer = new Trainer();
        trainer.setId(1L);
        trainer.setUser(user);
        trainer.setSpecialization(cardio);
    }

    @Test
    void registerCreatesTrainerWithSpecialization() {
        when(traineeDAO.existsByName("Mary", "Smith")).thenReturn(false);
        when(trainingTypeService.getById(1L)).thenReturn(cardio);
        when(userService.createUser("Mary", "Smith")).thenReturn(trainer.getUser());

        Trainer created = trainerService.register("Mary", "Smith", 1L);

        assertThat(created.getUser().getUsername()).isEqualTo("Mary.Smith");
        assertThat(created.getSpecialization()).isEqualTo(cardio);
        verify(trainerDAO).save(any(Trainer.class));
    }

    @Test
    void registerRejectsSomeoneAlreadyRegisteredAsTrainee() {
        when(traineeDAO.existsByName("Mary", "Smith")).thenReturn(true);

        assertThatThrownBy(() -> trainerService.register("Mary", "Smith", 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered as a trainee");

        verify(trainerDAO, never()).save(any());
    }

    @Test
    void getByUsernameThrowsWhenMissing() {
        when(trainerDAO.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.getByUsername("Ghost"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateKeepsUsernameAndSpecialization() {
        when(trainerDAO.findByUsername("Mary.Smith")).thenReturn(Optional.of(trainer));

        Trainer updated = trainerService.update("Mary.Smith", "Marianne", "Smithson", false);

        assertThat(updated.getUser().getUsername()).isEqualTo("Mary.Smith");
        assertThat(updated.getUser().getFirstName()).isEqualTo("Marianne");
        assertThat(updated.getSpecialization()).isEqualTo(cardio);
        assertThat(updated.getUser().isActive()).isFalse();
        verify(trainerDAO).update(trainer);
    }

    @Test
    void setActiveFlipsTheFlag() {
        when(trainerDAO.findByUsername("Mary.Smith")).thenReturn(Optional.of(trainer));

        trainerService.setActive("Mary.Smith", false);

        assertThat(trainer.getUser().isActive()).isFalse();
        verify(trainerDAO).update(trainer);
    }

    @Test
    void setActiveIsNotIdempotent() {
        when(trainerDAO.findByUsername("Mary.Smith")).thenReturn(Optional.of(trainer));

        assertThatThrownBy(() -> trainerService.setActive("Mary.Smith", true))
                .isInstanceOf(ConflictException.class);

        verify(trainerDAO, never()).update(any());
    }

    @Test
    void getTrainingsDelegatesFiltersToTheDao() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        when(trainerDAO.findByUsername("Mary.Smith")).thenReturn(Optional.of(trainer));
        when(trainerDAO.findTrainings(1L, from, null, "John")).thenReturn(List.of(new Training()));

        List<Training> trainings = trainerService.getTrainings("Mary.Smith", from, null, "John");

        assertThat(trainings).hasSize(1);
    }
}
