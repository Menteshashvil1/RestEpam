package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TraineeDAO;
import ge.epam.gymcrm.dao.TrainerDAO;
import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
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
class TraineeServiceTest {

    @Mock
    private TraineeDAO traineeDAO;

    @Mock
    private TrainerDAO trainerDAO;

    @Mock
    private UserService userService;

    @InjectMocks
    private TraineeService traineeService;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername("John.Doe");
        user.setPassword("secret1234");
        user.setActive(true);

        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setUser(user);
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 23));
        trainee.setAddress("Tbilisi");
    }

    private Trainer trainer(String username) {
        User user = new User();
        user.setFirstName("Mary");
        user.setLastName("Smith");
        user.setUsername(username);
        user.setActive(true);

        Trainer trainer = new Trainer();
        trainer.setId(2L);
        trainer.setUser(user);
        return trainer;
    }

    @Test
    void registerCreatesTraineeWithGeneratedCredentials() {
        when(trainerDAO.existsByName("John", "Doe")).thenReturn(false);
        when(userService.createUser("John", "Doe")).thenReturn(trainee.getUser());

        Trainee created = traineeService.register("John", "Doe", LocalDate.of(1995, 4, 23), "Tbilisi");

        assertThat(created.getUser().getUsername()).isEqualTo("John.Doe");
        assertThat(created.getAddress()).isEqualTo("Tbilisi");
        verify(traineeDAO).save(any(Trainee.class));
    }

    @Test
    void registerRejectsSomeoneAlreadyRegisteredAsTrainer() {
        when(trainerDAO.existsByName("John", "Doe")).thenReturn(true);

        assertThatThrownBy(() -> traineeService.register("John", "Doe", null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered as a trainer");

        verify(traineeDAO, never()).save(any());
    }

    @Test
    void getByUsernameThrowsWhenMissing() {
        when(traineeDAO.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.getByUsername("Nobody"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateChangesTheProfileButKeepsTheUsername() {
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        Trainee updated = traineeService.update("John.Doe", "Johnny", "Doer",
                LocalDate.of(1990, 1, 1), "Batumi", false);

        assertThat(updated.getUser().getUsername()).isEqualTo("John.Doe");
        assertThat(updated.getUser().getFirstName()).isEqualTo("Johnny");
        assertThat(updated.getUser().getLastName()).isEqualTo("Doer");
        assertThat(updated.getUser().isActive()).isFalse();
        assertThat(updated.getAddress()).isEqualTo("Batumi");
        verify(traineeDAO).update(trainee);
    }

    @Test
    void deleteRemovesTheProfile() {
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        traineeService.delete("John.Doe");

        verify(traineeDAO).delete(trainee);
    }

    @Test
    void setActiveFlipsTheFlag() {
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        traineeService.setActive("John.Doe", false);

        assertThat(trainee.getUser().isActive()).isFalse();
        verify(traineeDAO).update(trainee);
    }

    @Test
    void setActiveIsNotIdempotent() {
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        assertThatThrownBy(() -> traineeService.setActive("John.Doe", true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already active");

        verify(traineeDAO, never()).update(any());
    }

    @Test
    void getTrainingsDelegatesFiltersToTheDao() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(traineeDAO.findTrainings(1L, from, to, "Mary", "Cardio")).thenReturn(List.of(new Training()));

        List<Training> trainings = traineeService.getTrainings("John.Doe", from, to, "Mary", "Cardio");

        assertThat(trainings).hasSize(1);
        verify(traineeDAO).findTrainings(1L, from, to, "Mary", "Cardio");
    }

    @Test
    void getNotAssignedActiveTrainersDelegatesToTheDao() {
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(traineeDAO.findNotAssignedActiveTrainers(1L)).thenReturn(List.of(trainer("Mary.Smith")));

        List<Trainer> trainers = traineeService.getNotAssignedActiveTrainers("John.Doe");

        assertThat(trainers).extracting(t -> t.getUser().getUsername()).containsExactly("Mary.Smith");
    }

    @Test
    void updateTrainersReplacesTheWholeList() {
        Trainer mary = trainer("Mary.Smith");
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Mary.Smith")).thenReturn(Optional.of(mary));

        List<Trainer> result = traineeService.updateTrainers("John.Doe", List.of("Mary.Smith"));

        assertThat(result).containsExactly(mary);
        assertThat(trainee.getTrainers()).containsExactly(mary);
        verify(traineeDAO).update(trainee);
    }

    @Test
    void updateTrainersFailsOnUnknownTrainer() {
        when(traineeDAO.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.updateTrainers("John.Doe", List.of("Ghost")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ghost");
    }
}
