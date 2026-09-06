package ge.epam.gymcrm.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.dao.TrainingDAO;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.service.TrainingTypeService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainingSteps {

    private final GymApi api;
    private final ScenarioContext context;
    private final TrainingDAO trainingDAO;
    private final TransactionTemplate transactionTemplate;

    public TrainingSteps(MockMvc mockMvc, ObjectMapper objectMapper,
                         TrainingTypeService trainingTypeService, ScenarioContext context,
                         TrainingDAO trainingDAO, PlatformTransactionManager transactionManager) {
        this.api = new GymApi(mockMvc, objectMapper, trainingTypeService);
        this.context = context;
        this.trainingDAO = trainingDAO;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Given("a trainer {string} {string} and a trainee {string} {string} exist")
    public void aTrainerAndATraineeExist(String trainerFirstName, String trainerLastName,
                                         String traineeFirstName, String traineeLastName)
            throws Exception {
        MvcResult trainer = api.registerTrainer(trainerFirstName, trainerLastName);
        assertThat(trainer.getResponse().getStatus()).isEqualTo(201);
        context.setTrainerUsername(api.body(trainer).get("username").asText());
        context.setTrainerToken(api.body(trainer).get("token").asText());

        MvcResult trainee = api.registerTrainee(traineeFirstName, traineeLastName);
        assertThat(trainee.getResponse().getStatus()).isEqualTo(201);
        context.setTraineeUsername(api.body(trainee).get("username").asText());
        context.setTraineeToken(api.body(trainee).get("token").asText());
    }

    @When("a training {string} of {int} minutes is added on {string}")
    public void aTrainingIsAdded(String name, int duration, String date) throws Exception {
        context.setLastResult(api.addTraining(context.getTrainerToken(),
                context.getTraineeUsername(), context.getTrainerUsername(),
                name, LocalDate.parse(date), duration));
        rememberLatestTrainingId();
    }

    @Given("a training {string} of {int} minutes was added on {string}")
    public void aTrainingWasAdded(String name, int duration, String date) throws Exception {
        MvcResult result = api.addTraining(context.getTrainerToken(),
                context.getTraineeUsername(), context.getTrainerUsername(),
                name, LocalDate.parse(date), duration);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        context.setLastResult(result);
        rememberLatestTrainingId();
    }

    @When("a training is added for unknown trainee {string}")
    public void aTrainingIsAddedForUnknownTrainee(String traineeUsername) throws Exception {
        context.setLastResult(api.addTraining(context.getTrainerToken(), traineeUsername,
                context.getTrainerUsername(), "Morning cardio", LocalDate.of(2026, 7, 21), 60));
    }

    @When("that training is cancelled")
    public void thatTrainingIsCancelled() throws Exception {
        context.setLastResult(
                api.deleteTraining(context.getTrainerToken(), context.getLastTrainingId()));
    }

    @When("training {long} is cancelled")
    public void trainingIsCancelled(long trainingId) throws Exception {
        context.setLastResult(api.deleteTraining(context.getTrainerToken(), trainingId));
    }

    @When("the trainee profile is deleted")
    public void theTraineeProfileIsDeleted() throws Exception {
        context.setLastResult(
                api.deleteTrainee(context.getTraineeToken(), context.getTraineeUsername()));
    }

    @Then("the trainee has {int} training recorded")
    public void theTraineeHasOneTrainingRecorded(int expected) throws Exception {
        theTraineeHasTrainingsRecorded(expected);
    }

    @Then("the trainee has {int} trainings recorded")
    public void theTraineeHasTrainingsRecorded(int expected) throws Exception {
        MvcResult result =
                api.traineeTrainings(context.getTraineeToken(), context.getTraineeUsername());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(api.body(result)).hasSize(expected);
    }

    private void rememberLatestTrainingId() {
        transactionTemplate.executeWithoutResult(status -> {
            Optional<Training> latest = trainingDAO.findAll().stream()
                    .filter(training -> training.getTrainee().getUser().getUsername()
                            .equals(context.getTraineeUsername()))
                    .max(Comparator.comparing(Training::getId));
            latest.ifPresent(training -> context.setLastTrainingId(training.getId()));
        });
    }
}
