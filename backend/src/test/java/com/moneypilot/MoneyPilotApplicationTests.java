package com.moneypilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.moneypilot.common.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Full application context test backed by the configured local PostgreSQL.
 *
 * <p>This boots the complete Spring context — including the real datasource,
 * Flyway migrations against local PostgreSQL, Hibernate schema validation
 * ({@code ddl-auto=validate}), and all application beans such as
 * {@code AuthService} and the JPA repositories — and verifies the application
 * wires up end to end.
 *
 * <p>Per the approved architecture (backend-architecture §11, auth-lld §22),
 * integration tests run against local PostgreSQL; H2, Testcontainers, and Docker
 * are intentionally not used. A reachable local PostgreSQL instance is required
 * to run this test, consistent with the project's documented testing strategy.
 */
@SpringBootTest
class MoneyPilotApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // The full context started successfully. Confirm representative beans
        // from the web and persistence-backed service layers are present.
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBean(HealthController.class)).isNotNull();
        assertThat(applicationContext.getBean(com.moneypilot.auth.AuthService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.moneypilot.user.UserRepository.class)).isNotNull();
    }
}
