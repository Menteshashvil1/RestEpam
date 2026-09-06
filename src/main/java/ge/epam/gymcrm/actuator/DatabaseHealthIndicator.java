package ge.epam.gymcrm.actuator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                log.warn("Datasource connection reported invalid");
                return Health.down().withDetail("reason", "Connection is not valid").build();
            }
            DatabaseMetaData metaData = connection.getMetaData();
            return Health.up()
                    .withDetail("database", metaData.getDatabaseProductName())
                    .withDetail("version", metaData.getDatabaseProductVersion())
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down(e).build();
        }
    }
}
