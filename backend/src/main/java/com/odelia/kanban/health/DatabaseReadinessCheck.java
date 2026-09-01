package com.odelia.kanban.health;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * MicroProfile Health readiness probe: the application is ready once the
 * PostgreSQL data source answers. Exposed at {@code /health/ready}.
 */
@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    @Resource(lookup = "jdbc/kanban")
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            return HealthCheckResponse.builder()
                                      .name("kanban-database")
                                      .status(connection.isValid(2))
                                      .build();
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                                      .name("kanban-database")
                                      .down()
                                      .withData("error", String.valueOf(e.getMessage()))
                                      .build();
        }
    }
}
