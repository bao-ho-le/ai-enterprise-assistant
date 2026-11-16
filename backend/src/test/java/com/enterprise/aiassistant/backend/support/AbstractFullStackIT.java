package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.manager.QdrantCollectionManager;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * Base class for cross-layer workflow tests: real PostgreSQL, real MinIO, real Qdrant and the
 * full Spring context, with Gemini replaced by the deterministic embedding service.
 *
 * <p>All containers are singletons for the JVM, so the Spring context is built once and reused
 * by every subclass (same property set = same context cache key).
 */
@SpringBootTest
@Import(DeterministicEmbeddingTestConfig.class)
public abstract class AbstractFullStackIT {

    /** Base64 secret; JwtService requires a decodable key of at least 32 bytes. */
    private static final String JWT_SECRET_BASE64 =
            "dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy0xMjM0NTY3ODkw";

    /**
     * The application never creates the MinIO bucket, so the full-stack tests create it once.
     */
    @BeforeAll
    static void ensureStorageBucket() throws Exception {
        MinioContainerSupport.ensureBucket(MinioContainerSupport.newClient());
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected QdrantClient qdrantClient;

    @Autowired
    protected QdrantProperties qdrantProperties;

    @Autowired
    protected QdrantCollectionManager qdrantCollectionManager;

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {

        PostgresContainerSupport.registerDataSourceProperties(registry);
        MinioContainerSupport.registerProperties(registry);
        QdrantContainerSupport.registerProperties(registry);

        registry.add("api.prefix", () -> "/api/v1");
        registry.add("file.upload.max-size", () -> "500MB");

        // Dummy credentials: the deterministic embedding service is the only implementation
        // used, and the chat model bean is never invoked by in-scope tests.
        registry.add("gemini.api-key", () -> "test-key-not-used");
        registry.add("gemini.embedding-model", () -> "gemini-embedding-001");
        registry.add("gemini.llm-model.api-key", () -> "test-key-not-used");
        registry.add("gemini.llm-model.model-name", () -> "gemini-test-model");

        registry.add("jwt.access-token.secret", () -> JWT_SECRET_BASE64);
        registry.add("jwt.access-token.expiration", () -> 3_600_000L);
        registry.add("jwt.refresh-token.secret", () -> JWT_SECRET_BASE64);
        registry.add("jwt.refresh-token.expiration", () -> 604_800_000L);
    }

    /**
     * Removes document-related rows and empties the test vector collection so each test is
     * independent. Users, departments and the root folder created at startup are kept.
     */
    @BeforeEach
    void resetDocumentState() {

        // A previous test may have triggered asynchronous processing that is still writing rows.
        // Wait until no version is in flight so the deletes below cannot race an insert.
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> countInFlightVersions() == 0);

        jdbcTemplate.execute("UPDATE documents SET current_version_id = NULL");
        jdbcTemplate.execute("DELETE FROM document_accesses");
        jdbcTemplate.execute("DELETE FROM document_chunks");
        jdbcTemplate.execute("DELETE FROM document_texts");
        jdbcTemplate.execute("DELETE FROM document_versions");
        jdbcTemplate.execute("DELETE FROM documents");
        jdbcTemplate.execute("DELETE FROM files");

        String collection = qdrantProperties.getCollectionName();

        boolean exists = Futures.getUnchecked(qdrantClient.collectionExistsAsync(collection));

        if (exists) {
            Futures.getUnchecked(qdrantClient.deleteCollectionAsync(collection));
        }

        qdrantCollectionManager.initialize();
    }

    private Long countInFlightVersions() {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_versions WHERE status IN ('PENDING', 'PROCESSING')",
                Long.class);
    }
}
