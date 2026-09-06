package ge.epam.gymcrm.cucumber;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expected) {
        assertThat(context.getLastResult()).as("no request has been made yet").isNotNull();
        assertThat(context.getLastResult().getResponse().getStatus()).isEqualTo(expected);
    }
}
