package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QdrantVectorStoreService} with a mocked {@link QdrantClient}: request
 * construction, validation short-circuits and error translation. The real round-trip behaviour
 * is covered by {@code QdrantVectorStoreServiceIT}.
 */
class QdrantVectorStoreServiceTest {

    private static final String COLLECTION = "test_collection";

    private static final float[] VECTOR = {0.1f, 0.2f, 0.3f, 0.4f};

    private final QdrantClient qdrantClient = mock(QdrantClient.class);

    private QdrantProperties properties;

    private QdrantVectorStoreService service;

    @BeforeEach
    void setUp() {

        properties = new QdrantProperties();
        properties.setCollectionName(COLLECTION);
        properties.setVectorSize(VECTOR.length);
        properties.setScoreThreshold(0.4f);

        VectorStoreHelper helper = new VectorStoreHelper(properties);
        VectorStoreMapper mapper = new VectorStoreMapper(helper);

        service = new QdrantVectorStoreService(qdrantClient, properties, mapper, helper);
    }

    // ===================== upsert =====================

    @Test
    void shouldUpsertPointsIntoConfiguredCollection() {

        when(qdrantClient.upsertAsync(eq(COLLECTION), anyList()))
                .thenReturn(Futures.immediateFuture(Points.UpdateResult.getDefaultInstance()));

        service.upsert(List.of(point(1L), point(2L)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Points.PointStruct>> captor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertAsync(eq(COLLECTION), captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getId().getNum()).isEqualTo(1L);
        assertThat(captor.getValue().get(1).getId().getNum()).isEqualTo(2L);
    }

    @Test
    void shouldUpsertSinglePoint() {

        when(qdrantClient.upsertAsync(eq(COLLECTION), anyList()))
                .thenReturn(Futures.immediateFuture(Points.UpdateResult.getDefaultInstance()));

        service.upsert(point(9L));

        verify(qdrantClient).upsertAsync(eq(COLLECTION), anyList());
    }

    @Test
    void shouldRejectInvalidPointBeforeCallingQdrant() {

        VectorPoint wrongDimension = VectorPoint.builder()
                .id(1L)
                .vector(new float[]{1f})
                .payload(VectorPayload.builder().documentId(1L).build())
                .build();

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.upsert(List.of(wrongDimension))),
                ErrorCode.VECTOR_DIMENSION_INVALID
        );

        verifyNoInteractions(qdrantClient);
    }

    @Test
    void shouldRejectNullPointList() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.upsert((List<VectorPoint>) null)),
                ErrorCode.VECTOR_POINTS_REQUIRED
        );

        verifyNoInteractions(qdrantClient);
    }

    @Test
    void shouldTranslateUpsertFailure() {

        when(qdrantClient.upsertAsync(eq(COLLECTION), anyList()))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("qdrant down")));

        Throwable thrown = catchThrowable(() -> service.upsert(List.of(point(1L))));

        ErrorCodeAssertions.assertBusinessError(thrown, ErrorCode.VECTOR_UPSERT_FAILED);
        assertThat(thrown.getCause()).isNotNull();
    }

    // ===================== delete =====================

    @Test
    void shouldDeletePointFromConfiguredCollection() {

        when(qdrantClient.deleteAsync(eq(COLLECTION), anyList()))
                .thenReturn(Futures.immediateFuture(Points.UpdateResult.getDefaultInstance()));

        service.delete(12L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Points.PointId>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).deleteAsync(eq(COLLECTION), captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getNum()).isEqualTo(12L);
    }

    @Test
    void shouldRejectInvalidPointIdBeforeDeleting() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.delete(0L)),
                ErrorCode.VECTOR_POINT_ID_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.delete(null)),
                ErrorCode.VECTOR_POINT_ID_REQUIRED
        );

        verifyNoInteractions(qdrantClient);
    }

    @Test
    void shouldTranslateDeleteFailure() {

        when(qdrantClient.deleteAsync(eq(COLLECTION), anyList()))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("qdrant down")));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.delete(3L)),
                ErrorCode.VECTOR_DELETE_FAILED
        );
    }

    // ===================== search =====================

    @Test
    void shouldBuildSearchRequestAndMapHits() {

        Points.ScoredPoint hit = Points.ScoredPoint.newBuilder()
                .setId(PointIdFactory.id(42L))
                .setScore(0.88f)
                .putAllPayload(new VectorStoreMapper(new VectorStoreHelper(properties))
                        .toQdrantPayload(VectorPayload.builder()
                                .documentId(5L)
                                .documentVersionId(6L)
                                .chunkId(7L)
                                .content("text")
                                .build()))
                .build();

        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(List.of(hit)));

        List<SearchResult> results = service.search(VECTOR, 5, null);

        ArgumentCaptor<Points.SearchPoints> captor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(captor.capture());

        Points.SearchPoints request = captor.getValue();

        assertThat(request.getCollectionName()).isEqualTo(COLLECTION);
        assertThat(request.getVectorList()).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
        assertThat(request.getLimit()).isEqualTo(5);
        assertThat(request.getScoreThreshold()).isEqualTo(0.4f);
        assertThat(request.getWithPayload().getEnable()).isTrue();
        assertThat(request.hasFilter()).isFalse();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPointId()).isEqualTo(42L);
        assertThat(results.get(0).getScore()).isCloseTo(0.88, within(1e-6));
        assertThat(results.get(0).getPayload().getDocumentId()).isEqualTo(5L);
        assertThat(results.get(0).getPayload().getContent()).isEqualTo("text");
    }

    @Test
    void shouldApplyDocumentIdFilterWhenProvided() {

        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(List.of()));

        service.search(VECTOR, 3, 99L);

        ArgumentCaptor<Points.SearchPoints> captor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(captor.capture());

        Points.SearchPoints request = captor.getValue();

        assertThat(request.hasFilter()).isTrue();
        assertThat(request.getFilter().getMust(0).getField().getKey()).isEqualTo("documentId");
        assertThat(request.getFilter().getMust(0).getField().getMatch().getInteger()).isEqualTo(99L);
    }

    @Test
    void shouldReturnEmptyListWhenQdrantHasNoMatches() {

        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(List.of()));

        assertThat(service.search(VECTOR, 3, null)).isEmpty();
    }

    @Test
    void shouldRejectInvalidSearchRequestBeforeCallingQdrant() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(new float[0], 3, null)),
                ErrorCode.VECTOR_REQUIRED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(new float[]{1f}, 3, null)),
                ErrorCode.VECTOR_DIMENSION_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(VECTOR, 0, null)),
                ErrorCode.VECTOR_SEARCH_LIMIT_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(VECTOR, 3, 0L)),
                ErrorCode.DOCUMENT_ID_INVALID
        );

        verify(qdrantClient, never()).searchAsync(any(Points.SearchPoints.class));
    }

    @Test
    void shouldTranslateSearchFailure() {

        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("qdrant down")));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(VECTOR, 3, null)),
                ErrorCode.VECTOR_SEARCH_FAILED
        );
    }

    private static VectorPoint point(Long id) {

        return VectorPoint.builder()
                .id(id)
                .vector(VECTOR)
                .payload(VectorPayload.builder().documentId(1L).chunkId(id).content("c").build())
                .build();
    }
}
