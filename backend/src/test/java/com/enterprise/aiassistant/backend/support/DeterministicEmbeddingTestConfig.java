package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service.EmbeddingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the Gemini embedding client with the deterministic offline implementation inside
 * integration tests. No API key is read and no network call is made.
 */
@TestConfiguration
public class DeterministicEmbeddingTestConfig {

    @Bean
    @Primary
    public EmbeddingService deterministicEmbeddingService() {
        return new DeterministicEmbeddingService(QdrantContainerSupport.VECTOR_SIZE);
    }
}
