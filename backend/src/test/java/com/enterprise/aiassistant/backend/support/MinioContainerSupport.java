package com.enterprise.aiassistant.backend.support;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Singleton MinIO container with a dedicated bucket.
 *
 * <p>The image is pinned by digest (the same image already present on the machine) so the
 * suite never depends on a moving {@code latest} tag. The application itself never creates
 * the bucket, so the test support creates it once per JVM.
 */
public final class MinioContainerSupport {

    /** Verified, locally available digest of quay.io/minio/minio. */
    public static final String IMAGE =
            "quay.io/minio/minio@sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e";

    public static final int PORT = 9000;

    public static final String ACCESS_KEY = "test-access-key";

    public static final String SECRET_KEY = "test-secret-key-1234";

    public static final String BUCKET = "test-files";

    private static GenericContainer<?> container;

    private MinioContainerSupport() {
    }

    public static synchronized GenericContainer<?> get() {

        if (container == null) {

            GenericContainer<?> started = new GenericContainer<>(DockerImageName.parse(IMAGE))
                    .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
                    .withCommand("server /data")
                    .withExposedPorts(PORT)
                    .waitingFor(
                            Wait.forHttp("/minio/health/live")
                                    .forPort(PORT)
                                    .withStartupTimeout(Duration.ofSeconds(120))
                    );

            started.start();

            container = started;
        }

        return container;
    }

    public static String endpoint() {

        GenericContainer<?> minio = get();

        return "http://" + minio.getHost() + ":" + minio.getMappedPort(PORT);
    }

    public static MinioClient newClient() {

        return MinioClient.builder()
                .endpoint(endpoint())
                .credentials(ACCESS_KEY, SECRET_KEY)
                .build();
    }

    /**
     * Creates the test bucket if it does not exist yet.
     */
    public static void ensureBucket(MinioClient client) throws Exception {

        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(BUCKET).build()
        );

        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("minio.endpoint", MinioContainerSupport::endpoint);
        registry.add("minio.access-key", () -> ACCESS_KEY);
        registry.add("minio.secret-key", () -> SECRET_KEY);
        registry.add("minio.bucket", () -> BUCKET);
        registry.add("minio.provider", () -> "MINIO");
    }
}
