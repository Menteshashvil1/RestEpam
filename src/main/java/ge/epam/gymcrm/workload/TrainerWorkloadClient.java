package ge.epam.gymcrm.workload;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client that reaches the secondary microservice through Eureka (by service id).
 * Guarded by a Resilience4j circuit breaker; on failure {@link TrainerWorkloadClientFallback}
 * is used so a workload-service outage never breaks the main training flow.
 */
@FeignClient(name = "trainer-workload-service",
        configuration = WorkloadFeignConfig.class,
        fallbackFactory = TrainerWorkloadClientFallback.class)
public interface TrainerWorkloadClient {

    @PostMapping(value = "/api/v1/workload", consumes = MediaType.APPLICATION_JSON_VALUE)
    void sendWorkload(@RequestBody TrainerWorkloadRequest request);
}
