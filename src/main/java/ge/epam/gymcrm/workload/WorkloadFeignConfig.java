package ge.epam.gymcrm.workload;

import feign.RequestInterceptor;
import ge.epam.gymcrm.logging.TransactionContext;
import ge.epam.gymcrm.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Per-client Feign configuration (not {@code @Configuration}, so it is applied only to the
 * workload client). Attaches the microservice-to-microservice authorization and traceability
 * headers to every outbound call: a freshly minted service JWT (Bearer) and the transaction id.
 */
public class WorkloadFeignConfig {

    private final JwtService jwtService;
    private final String serviceSubject;

    public WorkloadFeignConfig(JwtService jwtService,
                               @Value("${spring.application.name:gym-crm-rest}") String serviceSubject) {
        this.jwtService = jwtService;
        this.serviceSubject = serviceSubject;
    }

    @Bean
    public RequestInterceptor workloadRequestInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + jwtService.generateToken(serviceSubject));

            String transactionId = TransactionContext.currentTransactionId();
            if (transactionId != null && !transactionId.isBlank()) {
                template.header(TransactionContext.TRANSACTION_ID_HEADER, transactionId);
            }
        };
    }
}
