package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config;

import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Qdrant client bean is created from configuration without requiring a running server.
 */
class QdrantConfigTest {

    @Test
    void shouldBuildClientFromConfiguredHostAndPort() {

        QdrantProperties properties = new QdrantProperties();
        properties.setHost("localhost");
        properties.setGrpcPort(6334);
        properties.setUseTls(false);
        properties.setCollectionName("test_document_chunks");
        properties.setVectorSize(8);

        QdrantClient client = new QdrantConfig(properties).qdrantClient();

        assertThat(client).isNotNull();

        client.close();
    }
}
