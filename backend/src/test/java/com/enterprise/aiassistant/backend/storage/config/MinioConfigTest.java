package com.enterprise.aiassistant.backend.storage.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the MinIO client bean is built from configuration without contacting the server.
 */
class MinioConfigTest {

    private final MinioConfig config = new MinioConfig();

    @Test
    void shouldCreateClientFromConfiguredEndpointAndCredentials() {

        MinioClient client = config.minioClient(
                "http://localhost:9000", "access-key", "secret-key");

        assertThat(client).isNotNull();
    }
}
