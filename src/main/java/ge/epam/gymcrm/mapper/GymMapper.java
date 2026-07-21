package ge.epam.gymcrm.mapper;

import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.dto.response.TraineeProfileResponse;
import ge.epam.gymcrm.dto.response.TraineeSummaryResponse;
import ge.epam.gymcrm.dto.response.TraineeTrainingResponse;
import ge.epam.gymcrm.dto.response.TrainerProfileResponse;
import ge.epam.gymcrm.dto.response.TrainerSummaryResponse;
import ge.epam.gymcrm.dto.response.TrainerTrainingResponse;
import ge.epam.gymcrm.dto.response.TrainingTypeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GymMapper {

    public TrainingTypeResponse toTrainingType(TrainingType type) {
        if (type == null) {
            return null;
        }
        return new TrainingTypeResponse(type.getId(), type.getTrainingTypeName());
    }

    public TrainerSummaryResponse toTrainerSummary(Trainer trainer) {
        return new TrainerSummaryResponse(
                trainer.getUser().getUsername(),
                trainer.getUser().getFirstName(),
                trainer.getUser().getLastName(),
                toTrainingType(trainer.getSpecialization()));
    }

    public TraineeSummaryResponse toTraineeSummary(Trainee trainee) {
        return new TraineeSummaryResponse(
                trainee.getUser().getUsername(),
                trainee.getUser().getFirstName(),
                trainee.getUser().getLastName());
    }

    public TraineeProfileResponse toTraineeProfile(Trainee trainee) {
        return new TraineeProfileResponse(
                trainee.getUser().getUsername(),
                trainee.getUser().getFirstName(),
                trainee.getUser().getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                trainee.getUser().isActive(),
                toTrainerSummaries(trainee.getTrainers()));
    }

    public TrainerProfileResponse toTrainerProfile(Trainer trainer) {
        return new TrainerProfileResponse(
                trainer.getUser().getUsername(),
                trainer.getUser().getFirstName(),
                trainer.getUser().getLastName(),
                toTrainingType(trainer.getSpecialization()),
                trainer.getUser().isActive(),
                trainer.getTrainees().stream().map(this::toTraineeSummary).toList());
    }

    public List<TrainerSummaryResponse> toTrainerSummaries(List<Trainer> trainers) {
        return trainers.stream().map(this::toTrainerSummary).toList();
    }

    public TraineeTrainingResponse toTraineeTraining(Training training) {
        return new TraineeTrainingResponse(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(),
                training.getTrainingDuration(),
                fullName(training.getTrainer().getUser().getFirstName(),
                        training.getTrainer().getUser().getLastName()));
    }

    public TrainerTrainingResponse toTrainerTraining(Training training) {
        return new TrainerTrainingResponse(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(),
                training.getTrainingDuration(),
                fullName(training.getTrainee().getUser().getFirstName(),
                        training.getTrainee().getUser().getLastName()));
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
