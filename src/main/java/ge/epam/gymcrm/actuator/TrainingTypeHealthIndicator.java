package ge.epam.gymcrm.actuator;

import ge.epam.gymcrm.dao.TrainingTypeDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component("trainingTypes")
public class TrainingTypeHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeHealthIndicator.class);

    private final TrainingTypeDAO trainingTypeDAO;
    private final TransactionTemplate transactionTemplate;

    public TrainingTypeHealthIndicator(TrainingTypeDAO trainingTypeDAO,
                                       PlatformTransactionManager transactionManager) {
        this.trainingTypeDAO = trainingTypeDAO;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(true);
    }

    @Override
    public Health health() {
        try {
            int count = transactionTemplate.execute(status -> trainingTypeDAO.findAll().size());
            if (count == 0) {
                return Health.down()
                        .withDetail("reason", "No training types are seeded")
                        .build();
            }
            return Health.up().withDetail("count", count).build();
        } catch (Exception e) {
            log.error("Training-type health check failed", e);
            return Health.down(e).build();
        }
    }
}
