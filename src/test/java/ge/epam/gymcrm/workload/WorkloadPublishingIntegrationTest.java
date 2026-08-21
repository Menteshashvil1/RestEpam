package ge.epam.gymcrm.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.logging.TransactionContext;
import ge.epam.gymcrm.security.JwtService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "gymcrm.messaging.workload-queue=test.workload.publishing")
class WorkloadPublishingIntegrationTest {

    @Autowired
    private WorkloadNotifier notifier;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${gymcrm.messaging.workload-queue}")
    private String workloadQueue;

    @BeforeEach
    void drainQueue() {
        jmsTemplate.setReceiveTimeout(200);
        while (jmsTemplate.receive(workloadQueue) != null) {
            continue;
        }
    }

    @AfterEach
    void clearTransactionId() {
        MDC.remove(TransactionContext.TRANSACTION_ID);
    }

    private Training training() {
        User user = new User();
        user.setFirstName("Mary");
        user.setLastName("Smith");
        user.setUsername("Mary.Smith");
        user.setActive(true);

        Trainer trainer = new Trainer();
        trainer.setUser(user);

        Training training = new Training();
        training.setTrainer(trainer);
        training.setTrainingName("Morning cardio");
        training.setTrainingDate(LocalDate.of(2026, 7, 21));
        training.setTrainingDuration(60);
        return training;
    }

    private Message receiveOne() {
        jmsTemplate.setReceiveTimeout(10_000);
        Message message = jmsTemplate.receive(workloadQueue);
        assertThat(message).as("expected a message on " + workloadQueue).isNotNull();
        return message;
    }

    private String receiveBody() throws JMSException {
        Message message = receiveOne();
        assertThat(message).isInstanceOf(TextMessage.class);
        return ((TextMessage) message).getText();
    }

    @Test
    void publishesTheAgreedJsonPayload() throws Exception {
        notifier.notifyAdded(training());

        assertThat(objectMapper.readTree(receiveBody())).isEqualTo(objectMapper.readTree("""
                {"trainerUsername":"Mary.Smith","trainerFirstName":"Mary","trainerLastName":"Smith",\
                "isActive":true,"trainingDate":"2026-07-21","trainingDuration":60,\
                "actionType":"ADD"}"""));
    }

    @Test
    void publishesTheLogicalTypeIdTheConsumerMapsToItsOwnClass() throws JMSException {
        notifier.notifyAdded(training());

        assertThat(receiveOne().getStringProperty(MessagingHeaders.TYPE_ID_PROPERTY))
                .isEqualTo(MessagingHeaders.WORKLOAD_TYPE_ID);
    }

    @Test
    void attachesAServiceJwtTheConsumerCanVerify() throws JMSException {
        notifier.notifyAdded(training());

        String token = receiveOne().getStringProperty(MessagingHeaders.AUTH_TOKEN);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("gym-crm-rest");
    }

    @Test
    void passesTheCurrentTransactionIdDownstream() throws JMSException {
        MDC.put(TransactionContext.TRANSACTION_ID, "tx-from-http-request");

        notifier.notifyAdded(training());

        assertThat(receiveOne().getStringProperty(MessagingHeaders.TRANSACTION_ID))
                .isEqualTo("tx-from-http-request");
    }

    @Test
    void omitsTheTransactionIdWhenThereIsNone() throws JMSException {
        notifier.notifyAdded(training());

        assertThat(receiveOne().getStringProperty(MessagingHeaders.TRANSACTION_ID)).isNull();
    }

    @Test
    void publishesADeleteActionWhenATrainingIsRemoved() throws Exception {
        notifier.notifyDeleted(training());

        assertThat(objectMapper.readTree(receiveBody()).get("actionType").asText())
                .isEqualTo("DELETE");
    }
}
