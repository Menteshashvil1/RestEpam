package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TraineeDAO;
import ge.epam.gymcrm.dao.TrainerDAO;
import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.ConflictException;
import ge.epam.gymcrm.exception.NotFoundException;
import ge.epam.gymcrm.metrics.GymMetrics;
import ge.epam.gymcrm.workload.WorkloadNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDAO traineeDAO;
    private TrainerDAO trainerDAO;
    private UserService userService;
    private GymMetrics metrics;
    private WorkloadNotifier workloadNotifier;

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) { this.traineeDAO = traineeDAO; }

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) { this.trainerDAO = trainerDAO; }

    @Autowired
    public void setUserService(UserService userService) { this.userService = userService; }

    @Autowired
    public void setMetrics(GymMetrics metrics) { this.metrics = metrics; }

    @Autowired
    public void setWorkloadNotifier(WorkloadNotifier workloadNotifier) {
        this.workloadNotifier = workloadNotifier;
    }

    public Trainee register(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        if (trainerDAO.existsByName(firstName, lastName)) {
            throw new ConflictException(
                    "%s %s is already registered as a trainer and cannot also be a trainee"
                            .formatted(firstName, lastName));
        }

        User user = userService.createUser(firstName, lastName);
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        traineeDAO.save(trainee);
        metrics.recordTraineeRegistration();

        log.info("Registered trainee with username: {}", user.getUsername());
        return trainee;
    }

    @Transactional(readOnly = true)
    public long count() {
        return traineeDAO.count();
    }

    @Transactional(readOnly = true)
    public Trainee getByUsername(String username) {
        return traineeDAO.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Trainee not found: " + username));
    }

    public Trainee update(String username, String firstName, String lastName,
                          LocalDate dateOfBirth, String address, boolean isActive) {
        Trainee trainee = getByUsername(username);
        trainee.getUser().setFirstName(firstName);
        trainee.getUser().setLastName(lastName);
        trainee.getUser().setActive(isActive);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        traineeDAO.update(trainee);

        log.info("Updated trainee profile: {}", username);
        return trainee;
    }

    public void delete(String username) {
        Trainee trainee = getByUsername(username);

        for (Training training : new ArrayList<>(trainee.getTrainings())) {
            workloadNotifier.notifyDeleted(training);
        }

        traineeDAO.delete(trainee);
        log.info("Deleted trainee profile: {}", username);
    }

    public void setActive(String username, boolean isActive) {
        Trainee trainee = getByUsername(username);
        if (trainee.getUser().isActive() == isActive) {
            throw new ConflictException(
                    "Trainee %s is already %s".formatted(username, isActive ? "active" : "de-activated"));
        }
        trainee.getUser().setActive(isActive);
        traineeDAO.update(trainee);
        log.info("Trainee {} isActive={}", username, isActive);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainings(String username, LocalDate fromDate, LocalDate toDate,
                                       String trainerName, String trainingType) {
        Trainee trainee = getByUsername(username);
        return traineeDAO.findTrainings(trainee.getId(), fromDate, toDate, trainerName, trainingType);
    }

    @Transactional(readOnly = true)
    public List<Trainer> getNotAssignedActiveTrainers(String username) {
        Trainee trainee = getByUsername(username);
        return traineeDAO.findNotAssignedActiveTrainers(trainee.getId());
    }

    public List<Trainer> updateTrainers(String username, List<String> trainerUsernames) {
        Trainee trainee = getByUsername(username);
        List<Trainer> trainers = new ArrayList<>();
        for (String trainerUsername : trainerUsernames) {
            trainers.add(trainerDAO.findByUsername(trainerUsername)
                    .orElseThrow(() -> new NotFoundException("Trainer not found: " + trainerUsername)));
        }
        trainee.setTrainers(trainers);
        traineeDAO.update(trainee);

        log.info("Updated trainer list for trainee {}: {}", username, trainerUsernames);
        return trainers;
    }
}
