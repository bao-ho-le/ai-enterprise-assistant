package com.enterprise.aiassistant.backend.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for JPA-slice integration tests against a real PostgreSQL container.
 *
 * <p>{@code replace = NONE} keeps the Testcontainers datasource (no embedded database is
 * used, so PostgreSQL semantics - CAST, LOWER/ILIKE, unique constraints - are real).
 * Spring's default transaction rollback per test keeps the database clean.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPostgresIT {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        PostgresContainerSupport.registerDataSourceProperties(registry);
    }
}
