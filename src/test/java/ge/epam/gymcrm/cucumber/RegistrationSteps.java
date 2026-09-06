package ge.epam.gymcrm.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.service.TrainingTypeService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

public class RegistrationSteps {

    private final GymApi api;
    private final ScenarioContext context;

    public RegistrationSteps(MockMvc mockMvc, ObjectMapper objectMapper,
                             TrainingTypeService trainingTypeService, ScenarioContext context) {
        this.api = new GymApi(mockMvc, objectMapper, trainingTypeService);
        this.context = context;
    }

    @When("a trainee registers with first name {string} and last name {string}")
    public void aTraineeRegisters(String firstName, String lastName) throws Exception {
        MvcResult result = api.registerTrainee(firstName, lastName);
        context.setLastResult(result);
        rememberTrainee(result);
    }

    @When("a trainer registers with first name {string} and last name {string}")
    public void aTrainerRegisters(String firstName, String lastName) throws Exception {
        MvcResult result = api.registerTrainer(firstName, lastName);
        context.setLastResult(result);
        rememberTrainer(result);
    }

    @Given("a trainer is registered with first name {string} and last name {string}")
    public void aTrainerIsRegistered(String firstName, String lastName) throws Exception {
        MvcResult result = api.registerTrainer(firstName, lastName);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        rememberTrainer(result);
    }

    @When("that user logs in with the generated password")
    public void thatUserLogsInWithTheGeneratedPassword() throws Exception {
        context.setLastResult(api.login(context.getTrainerUsername(), context.getTrainerPassword()));
    }

    @When("that user logs in with password {string}")
    public void thatUserLogsInWithPassword(String password) throws Exception {
        context.setLastResult(api.login(context.getTrainerUsername(), password));
    }

    @When("the trainer profile is requested without a token")
    public void theTrainerProfileIsRequestedWithoutAToken() throws Exception {
        context.setLastResult(api.trainerProfile(null, context.getTrainerUsername()));
    }

    @When("the trainer profile is requested with the returned token")
    public void theTrainerProfileIsRequestedWithTheReturnedToken() throws Exception {
        context.setLastResult(
                api.trainerProfile(context.getTrainerToken(), context.getTrainerUsername()));
    }

    @Then("the returned username starts with {string}")
    public void theReturnedUsernameStartsWith(String expected) throws Exception {
        assertThat(body().get("username").asText()).startsWith(expected);
    }

    @Then("the returned first name is {string}")
    public void theReturnedFirstNameIs(String expected) throws Exception {
        assertThat(body().get("firstName").asText()).isEqualTo(expected);
    }

    @Then("a password and a token are returned")
    public void aPasswordAndATokenAreReturned() throws Exception {
        assertThat(body().get("password").asText()).isNotBlank();
        assertThat(body().get("token").asText()).isNotBlank();
    }

    @Then("a token is returned")
    public void aTokenIsReturned() throws Exception {
        assertThat(body().get("token").asText()).isNotBlank();
    }

    private JsonNode body() throws Exception {
        return api.body(context.getLastResult());
    }

    private void rememberTrainee(MvcResult result) throws Exception {
        if (result.getResponse().getStatus() != 201) {
            return;
        }
        JsonNode body = api.body(result);
        context.setTraineeUsername(body.get("username").asText());
        context.setTraineePassword(body.get("password").asText());
        context.setTraineeToken(body.get("token").asText());
    }

    private void rememberTrainer(MvcResult result) throws Exception {
        if (result.getResponse().getStatus() != 201) {
            return;
        }
        JsonNode body = api.body(result);
        context.setTrainerUsername(body.get("username").asText());
        context.setTrainerPassword(body.get("password").asText());
        context.setTrainerToken(body.get("token").asText());
    }
}
