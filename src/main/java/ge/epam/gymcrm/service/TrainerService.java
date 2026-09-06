package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TraineeDAO;
import ge.epam.gymcrm.dao.TrainerDAO;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.ConflictException;
import ge.epam.gymcrm.exception.NotFoundException;
import ge.epam.gymcrm.metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDAO trainerDAO;
    private TraineeDAO traineeDAO;
    private TrainingTypeService trainingTypeService;
    private UserService userService;
    private GymMetrics metrics;

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) { this.trainerDAO = trainerDAO; }

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) { this.traineeDAO = traineeDAO; }

    @Autowired
    public void setTrainingTypeService(TrainingTypeService trainingTypeService) {
        this.trainingTypeService = trainingTypeService;
    }

    @Autowired
    public void setUserService(UserService userService) { this.userService = userService; }

    @Autowired
    public void setMetrics(GymMetrics metrics) { this.metrics = metrics; }

    public Trainer register(String firstName, String lastName, Long specializationId) {
        if (traineeDAO.existsByName(firstName, lastName)) {
            throw new ConflictException(
                    "%s %s is already registered as a trainee and cannot also be a trainer"
                            .formatted(firstName, lastName));
        }
        TrainingType specialization = trainingTypeService.getById(specializationId);

        User user = userService.createUser(firstName, lastName);
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setSpecialization(specialization);
        trainerDAO.save(trainer);
        metrics.recordTrainerRegistration();

        log.info("Registered trainer with username: {}", user.getUsername());
        return trainer;
    }

    @Transactional(readOnly = true)
    public long count() {
        return trainerDAO.count();
    }

    @Transactional(readOnly = true)
    public Trainer getByUsername(String username) {
        return trainerDAO.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Trainer not found: " + username));
    }

    public Trainer update(String username, String firstName, String lastName, boolean isActive) {
        Trainer trainer = getByUsername(username);
        trainer.getUser().setFirstName(firstName);
        trainer.getUser().setLastName(lastName);
        trainer.getUser().setActive(isActive);
        trainerDAO.update(trainer);

        log.info("Updated trainer profile: {}", username);
        return trainer;
    }

    public void setActive(String username, boolean isActive) {
        Trainer trainer = getByUsername(username);
        if (trainer.getUser().isActive() == isActive) {
            throw new ConflictException(
                    "Trainer %s is already %s".formatted(username, isActive ? "active" : "de-activated"));
        }
        trainer.getUser().setActive(isActive);
        trainerDAO.update(trainer);
        log.info("Trainer {} isActive={}", username, isActive);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainings(String username, LocalDate fromDate, LocalDate toDate,
                                       String traineeName) {
        Trainer trainer = getByUsername(username);
        return trainerDAO.findTrainings(trainer.getId(), fromDate, toDate, traineeName);
    }
}
