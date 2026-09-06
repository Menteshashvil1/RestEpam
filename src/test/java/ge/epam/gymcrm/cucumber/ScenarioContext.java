package ge.epam.gymcrm.cucumber;

import io.cucumber.spring.ScenarioScope;
import jakarta.jms.Message;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

@Component
@ScenarioScope
public class ScenarioContext {

    private MvcResult lastResult;
    private String traineeUsername;
    private String traineePassword;
    private String traineeToken;
    private String trainerUsername;
    private String trainerPassword;
    private String trainerToken;
    private Long lastTrainingId;
    private final List<Message> publishedEvents = new ArrayList<>();

    public MvcResult getLastResult() {
        return lastResult;
    }

    public void setLastResult(MvcResult lastResult) {
        this.lastResult = lastResult;
    }

    public String getTraineeUsername() {
        return traineeUsername;
    }

    public void setTraineeUsername(String traineeUsername) {
        this.traineeUsername = traineeUsername;
    }

    public String getTraineePassword() {
        return traineePassword;
    }

    public void setTraineePassword(String traineePassword) {
        this.traineePassword = traineePassword;
    }

    public String getTraineeToken() {
        return traineeToken;
    }

    public void setTraineeToken(String traineeToken) {
        this.traineeToken = traineeToken;
    }

    public String getTrainerUsername() {
        return trainerUsername;
    }

    public void setTrainerUsername(String trainerUsername) {
        this.trainerUsername = trainerUsername;
    }

    public String getTrainerPassword() {
        return trainerPassword;
    }

    public void setTrainerPassword(String trainerPassword) {
        this.trainerPassword = trainerPassword;
    }

    public String getTrainerToken() {
        return trainerToken;
    }

    public void setTrainerToken(String trainerToken) {
        this.trainerToken = trainerToken;
    }

    public Long getLastTrainingId() {
        return lastTrainingId;
    }

    public void setLastTrainingId(Long lastTrainingId) {
        this.lastTrainingId = lastTrainingId;
    }

    public List<Message> getPublishedEvents() {
        return publishedEvents;
    }
}
