package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import io.qdrant.client.QdrantClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Singleton Qdrant container plus the test-only {@link QdrantProperties} pointing at it.
 *
 * <p>Never uses the development collection ({@code document_chunks}): tests use
 * {@value #COLLECTION_NAME} with a small deterministic dimension ({@value #VECTOR_SIZE}).
 */
public final class QdrantContainerSupport {

    public static final String IMAGE = "qdrant/qdrant:v1.15.1";

    public static final int GRPC_PORT = 6334;

    public static final int HTTP_PORT = 6333;

    public static final String COLLECTION_NAME = "test_document_chunks";

    public static final int VECTOR_SIZE = 8;

    private static GenericContainer<?> container;

    private QdrantContainerSupport() {
    }

    public static synchronized GenericContainer<?> get() {

        if (container == null) {

            GenericContainer<?> started = new GenericContainer<>(DockerImageName.parse(IMAGE))
                    .withExposedPorts(GRPC_PORT, HTTP_PORT)
                    .waitingFor(
                            Wait.forListeningPorts(GRPC_PORT, HTTP_PORT)
                                    .withStartupTimeout(Duration.ofSeconds(120))
                    );

            started.start();

            container = started;
        }

        return container;
    }

    public static String host() {
        return get().getHost();
    }

    public static int grpcPort() {
        return get().getMappedPort(GRPC_PORT);
    }

    public static QdrantProperties properties() {

        QdrantProperties properties = new QdrantProperties();

        properties.setHost(host());
        properties.setGrpcPort(grpcPort());
        properties.setRestPort(get().getMappedPort(HTTP_PORT));
        properties.setCollectionName(COLLECTION_NAME);
        properties.setVectorSize(VECTOR_SIZE);
        properties.setScoreThreshold(0.0f);
        properties.setUseTls(false);

        return properties;
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("qdrant.host", QdrantContainerSupport::host);
        registry.add("qdrant.grpc-port", QdrantContainerSupport::grpcPort);
        registry.add("qdrant.rest-port", () -> get().getMappedPort(HTTP_PORT));
        registry.add("qdrant.collection-name", () -> COLLECTION_NAME);
        registry.add("qdrant.vector-size", () -> VECTOR_SIZE);
        registry.add("qdrant.score-threshold", () -> 0.0f);
        registry.add("qdrant.use-tls", () -> false);
    }

    /**
     * Blocks until the gRPC endpoint answers a health check (no arbitrary sleeps).
     */
    public static void awaitReady(QdrantClient client) {

        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        RuntimeException lastFailure = null;

        while (System.nanoTime() < deadline) {

            try {
                com.google.common.util.concurrent.Futures.getUnchecked(client.healthCheckAsync());
                return;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                try {
                    Thread.sleep(250);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Qdrant", interrupted);
                }
            }
        }

        throw new IllegalStateException("Qdrant did not become ready in time", lastFailure);
    }
}
