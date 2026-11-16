package com.enterprise.aiassistant.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton PostgreSQL container shared by every integration test in the JVM.
 *
 * <p>Uses the same major version as docker-compose (postgres:16-alpine) but an isolated
 * database and credentials, so development data is never touched.
 *
 * <p>The schema is created by Hibernate ({@code create-drop}) because the repository's Flyway
 * migrations do not create the document/file/folder tables (see docs/TESTING.md).
 * Flyway is explicitly disabled for tests only.
 */
public final class PostgresContainerSupport {

    public static final String IMAGE = "postgres:16-alpine";

    public static final String DATABASE = "ai_assistant_test";

    public static final String USERNAME = "test_user";

    public static final String PASSWORD = "test_password";

    private static PostgreSQLContainer container;

    private PostgresContainerSupport() {
    }

    public static synchronized PostgreSQLContainer get() {

        if (container == null) {

            PostgreSQLContainer started = new PostgreSQLContainer(DockerImageName.parse(IMAGE))
                    .withDatabaseName(DATABASE)
                    .withUsername(USERNAME)
                    .withPassword(PASSWORD);

            started.start();

            container = started;
        }

        return container;
    }

    /**
     * Registers datasource + JPA test properties. Never points at the development database.
     */
    public static void registerDataSourceProperties(DynamicPropertyRegistry registry) {

        PostgreSQLContainer postgres = get();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Test-only schema strategy: Flyway migrations are incomplete for document tables.
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
        registry.add("spring.jpa.show-sql", () -> false);
    }
}
