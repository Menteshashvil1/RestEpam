package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TrainingDAO;
import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDAO trainingDAO;
    private TraineeService traineeService;
    private TrainerService trainerService;

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) { this.trainingDAO = trainingDAO; }

    @Autowired
    public void setTraineeService(TraineeService traineeService) { this.traineeService = traineeService; }

    @Autowired
    public void setTrainerService(TrainerService trainerService) { this.trainerService = trainerService; }

    /**
     * Adds a training. The training type is not part of the request — it is taken from the
     * trainer's specialization. Trainings can neither be updated nor deleted via REST.
     */
    public Training addTraining(String traineeUsername, String trainerUsername, String trainingName,
                                LocalDate trainingDate, int trainingDuration) {
        Trainee trainee = traineeService.getByUsername(traineeUsername);
        Trainer trainer = trainerService.getByUsername(trainerUsername);

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingName(trainingName);
        training.setTrainingType(trainer.getSpecialization());
        training.setTrainingDate(trainingDate);
        training.setTrainingDuration(trainingDuration);
        trainingDAO.save(training);

        // Adding a training implicitly assigns the trainer to the trainee.
        if (!trainee.getTrainers().contains(trainer)) {
            trainee.getTrainers().add(trainer);
        }

        log.info("Added training '{}' for trainee {} with trainer {}",
                trainingName, traineeUsername, trainerUsername);
        return training;
    }
}
