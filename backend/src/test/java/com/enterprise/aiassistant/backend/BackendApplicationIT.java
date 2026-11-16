package com.enterprise.aiassistant.backend;

import com.enterprise.aiassistant.backend.support.AbstractFullStackIT;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the whole Spring context starts against real infrastructure (PostgreSQL, MinIO,
 * Qdrant) and that startup initializers created the vector collection.
 *
 * <p>Converted from {@code @SpringBootTest} to a full-stack IT so that {@code mvn test} never
 * needs Docker.
 */
class BackendApplicationIT extends AbstractFullStackIT {

    @Test
    void contextLoadsWithAllInfrastructureAvailable() {

        assertThat(Futures.getUnchecked(
                qdrantClient.collectionExistsAsync(qdrantProperties.getCollectionName())))
                .isTrue();
    }
}
