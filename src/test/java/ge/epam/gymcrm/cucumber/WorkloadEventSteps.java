package ge.epam.gymcrm.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.security.JwtService;
import ge.epam.gymcrm.workload.MessagingHeaders;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkloadEventSteps {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final ScenarioContext context;
    private final String workloadQueue;

    public WorkloadEventSteps(JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                              JwtService jwtService, ScenarioContext context,
                              @Value("${gymcrm.messaging.workload-queue}") String workloadQueue) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
        this.context = context;
        this.workloadQueue = workloadQueue;
    }

    @Given("the workload queue is empty")
    public void theWorkloadQueueIsEmpty() {
        jmsTemplate.setReceiveTimeout(200);
        while (jmsTemplate.receive(workloadQueue) != null) {
            continue;
        }
        context.getPublishedEvents().clear();
    }

    @Then("an event is published to the workload queue")
    public void anEventIsPublished() {
        Message message = receive();
        assertThat(message).as("expected an event on " + workloadQueue).isNotNull();
        context.getPublishedEvents().add(message);
    }

    @Then("{int} events are published to the workload queue")
    public void eventsArePublished(int expected) {
        for (int i = 0; i < expected; i++) {
            Message message = receive();
            assertThat(message).as("expected %d events, got %d", expected, i).isNotNull();
            context.getPublishedEvents().add(message);
        }
        assertThat(receive()).as("more events than the expected %d", expected).isNull();
    }

    @Then("no event is published to the workload queue")
    public void noEventIsPublished() {
        assertThat(receive()).as("expected nothing on " + workloadQueue).isNull();
    }

    @Then("the event field {string} is {string}")
    public void theEventFieldIs(String field, String expected) throws Exception {
        assertThat(payload(lastEvent()).get(field).asText()).isEqualTo(expected);
    }

    @Then("the event names the registered trainer")
    public void theEventNamesTheRegisteredTrainer() throws Exception {
        assertThat(payload(lastEvent()).get("trainerUsername").asText())
                .isEqualTo(context.getTrainerUsername());
    }

    @Then("the event carries the logical type id the workload service maps")
    public void theEventCarriesTheLogicalTypeId() throws JMSException {
        assertThat(lastEvent().getStringProperty(MessagingHeaders.TYPE_ID_PROPERTY))
                .isEqualTo(MessagingHeaders.WORKLOAD_TYPE_ID);
    }

    @Then("the event carries a service token the workload service can verify")
    public void theEventCarriesAServiceToken() throws JMSException {
        String token = lastEvent().getStringProperty(MessagingHeaders.AUTH_TOKEN);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("gym-crm-rest");
    }

    @Then("the event carries a transaction id")
    public void theEventCarriesATransactionId() throws JMSException {
        assertThat(lastEvent().getStringProperty(MessagingHeaders.TRANSACTION_ID)).isNotBlank();
    }

    @Then("every published event has action type {string}")
    public void everyPublishedEventHasActionType(String expected) throws Exception {
        assertThat(context.getPublishedEvents()).isNotEmpty();
        for (Message event : context.getPublishedEvents()) {
            assertThat(payload(event).get("actionType").asText()).isEqualTo(expected);
        }
    }

    @Then("the event body has exactly the agreed fields")
    public void theEventBodyHasExactlyTheAgreedFields() throws Exception {
        List<String> fields = new ArrayList<>();
        payload(lastEvent()).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("trainerUsername", "trainerFirstName",
                "trainerLastName", "isActive", "trainingDate", "trainingDuration", "actionType");
    }

    private Message receive() {
        jmsTemplate.setReceiveTimeout(2_000);
        return jmsTemplate.receive(workloadQueue);
    }

    private Message lastEvent() {
        assertThat(context.getPublishedEvents()).as("no event has been received").isNotEmpty();
        return context.getPublishedEvents().get(context.getPublishedEvents().size() - 1);
    }

    private JsonNode payload(Message message) throws Exception {
        return objectMapper.readTree(((TextMessage) message).getText());
    }
}
