package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.manager.QdrantCollectionManager;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.enterprise.aiassistant.backend.support.QdrantContainerSupport;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests against a real Qdrant container. Uses a dedicated collection
 * ({@value QdrantContainerSupport#COLLECTION_NAME}) with an 8-dimensional vector, never the
 * production collection.
 */
class QdrantVectorStoreServiceIT {

    private static QdrantClient client;

    private static QdrantProperties properties;

    private QdrantVectorStoreService service;

    private QdrantCollectionManager collectionManager;

    @BeforeAll
    static void connect() {

        properties = QdrantContainerSupport.properties();

        client = new QdrantClient(QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getGrpcPort(),
                properties.getUseTls()
        ).build());

        QdrantContainerSupport.awaitReady(client);
    }

    @BeforeEach
    void setUp() {

        VectorStoreHelper helper = new VectorStoreHelper(properties);
        VectorStoreMapper mapper = new VectorStoreMapper(helper);

        service = new QdrantVectorStoreService(client, properties, mapper, helper);
        collectionManager = new QdrantCollectionManager(client, properties);

        recreateCollection();
    }

    @AfterEach
    void tearDown() {

        if (Futures.getUnchecked(client.collectionExistsAsync(properties.getCollectionName()))) {
            Futures.getUnchecked(client.deleteCollectionAsync(properties.getCollectionName()));
        }
    }

    @Test
    void shouldInitializeCollectionIdempotently() {

        // The collection already exists because setUp() recreated it; a second call must not fail.
        collectionManager.initialize();
        collectionManager.initialize();

        assertThat(Futures.getUnchecked(client.collectionExistsAsync(properties.getCollectionName())))
                .isTrue();
    }

    @Test
    void shouldCreateCollectionWhenMissing() {

        Futures.getUnchecked(client.deleteCollectionAsync(properties.getCollectionName()));

        collectionManager.initialize();

        assertThat(Futures.getUnchecked(client.collectionExistsAsync(properties.getCollectionName())))
                .isTrue();
    }

    @Test
    void shouldUpsertAndRetrieveByCosineSimilarity() {

        float[] target = unitVector(0);

        service.upsert(List.of(
                point(1L, 10L, target, "target"),
                point(2L, 10L, unitVector(1), "neighbour")
        ));

        List<SearchResult> results = service.search(target, 5, null);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getPointId()).isEqualTo(1L);
        assertThat(results.get(0).getScore()).isCloseTo(1.0, within(1e-4));
        assertThat(results.get(0).getPayload().getContent()).isEqualTo("target");
    }

    @Test
    void shouldOrderResultsByDescendingScore() {

        // Every component stays positive so all cosine similarities are > 0 and survive the
        // 0.0 score threshold used by the test configuration.
        float[] query = blendedVector(1.0f);

        service.upsert(List.of(
                point(1L, 10L, blendedVector(0.1f), "far"),
                point(2L, 10L, blendedVector(0.9f), "exact"),
                point(3L, 10L, blendedVector(0.5f), "neighbour")
        ));

        List<SearchResult> results = service.search(query, 3, null);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getPointId()).isEqualTo(2L);
        assertThat(results.get(2).getPointId()).isEqualTo(1L);

        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i - 1).getScore())
                    .isGreaterThanOrEqualTo(results.get(i).getScore());
        }
    }

    @Test
    void shouldFilterByDocumentId() {

        service.upsert(List.of(
                point(1L, 100L, unitVector(0), "doc-100"),
                point(2L, 200L, unitVector(0), "doc-200")
        ));

        List<SearchResult> results = service.search(unitVector(0), 10, 200L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPayload().getDocumentId()).isEqualTo(200L);
        assertThat(results.get(0).getPayload().getContent()).isEqualTo("doc-200");
    }

    @Test
    void shouldRoundTripEveryPayloadField() {

        VectorPayload payload = VectorPayload.builder()
                .chunkId(55L)
                .documentId(66L)
                .documentVersionId(77L)
                .chunkIndex(3)
                .pageNumber(4)
                .startChar(100)
                .endChar(250)
                .tokenCount(37)
                .embeddingModel("deterministic-test-embedding")
                .content("payload round trip")
                .build();

        service.upsert(VectorPoint.builder()
                .id(9L)
                .vector(unitVector(0))
                .payload(payload)
                .build());

        SearchResult result = service.search(unitVector(0), 1, 66L).get(0);

        assertThat(result.getPayload()).isEqualTo(payload);
    }

    @Test
    void shouldDeletePoint() {

        service.upsert(List.of(
                point(1L, 10L, unitVector(0), "keep"),
                point(2L, 10L, unitVector(1), "delete")
        ));

        service.delete(2L);

        assertThat(service.search(unitVector(0), 10, null))
                .extracting(SearchResult::getPointId)
                .containsExactly(1L);
    }

    @Test
    void shouldReturnEmptyListWhenCollectionIsEmpty() {

        assertThat(service.search(unitVector(0), 5, null)).isEmpty();
    }

    @Test
    void shouldRejectQueryWithWrongDimension() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(new float[]{1f, 2f}, 5, null)),
                ErrorCode.VECTOR_DIMENSION_INVALID
        );
    }

    @Test
    void shouldRejectUpsertWithWrongDimension() {

        VectorPoint wrong = VectorPoint.builder()
                .id(1L)
                .vector(new float[]{1f})
                .payload(VectorPayload.builder().documentId(1L).build())
                .build();

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.upsert(List.of(wrong))),
                ErrorCode.VECTOR_DIMENSION_INVALID
        );
    }

    // ===================== helpers =====================

    private void recreateCollection() {

        String collection = properties.getCollectionName();

        if (Futures.getUnchecked(client.collectionExistsAsync(collection))) {
            Futures.getUnchecked(client.deleteCollectionAsync(collection));
        }

        collectionManager.initialize();
    }

    private static VectorPoint point(Long id, Long documentId, float[] vector, String content) {

        return VectorPoint.builder()
                .id(id)
                .vector(vector)
                .payload(VectorPayload.builder()
                        .chunkId(id)
                        .documentId(documentId)
                        .documentVersionId(1L)
                        .content(content)
                        .build())
                .build();
    }

    /**
     * Deterministic unit vector: component {@code index} is 1, everything else 0.
     */
    private static float[] unitVector(int index) {

        float[] vector = new float[QdrantContainerSupport.VECTOR_SIZE];

        vector[index] = 1f;

        return vector;
    }

    /**
     * Vector with a configurable first component and 0.2 elsewhere, so the cosine similarity to
     * another blended vector is strictly positive and ordered by the first component.
     */
    private static float[] blendedVector(float firstComponent) {

        float[] vector = new float[QdrantContainerSupport.VECTOR_SIZE];

        java.util.Arrays.fill(vector, 0.2f);
        vector[0] = firstComponent;

        return vector;
    }
}
