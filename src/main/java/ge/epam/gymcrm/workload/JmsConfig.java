package ge.epam.gymcrm.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
public class JmsConfig {

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName(MessagingHeaders.TYPE_ID_PROPERTY);
        converter.setTypeIdMappings(
                Map.of(MessagingHeaders.WORKLOAD_TYPE_ID, TrainerWorkloadRequest.class));
        return converter;
    }

    @Bean
    public ActiveMQConnectionFactoryCustomizer sendTimeoutCustomizer(
            @Value("${gymcrm.messaging.send-timeout-ms:5000}") int sendTimeoutMs) {
        return factory -> factory.setSendTimeout(sendTimeoutMs);
    }
}
