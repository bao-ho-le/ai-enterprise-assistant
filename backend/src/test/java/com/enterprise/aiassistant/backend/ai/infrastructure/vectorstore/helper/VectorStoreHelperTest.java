package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Unit tests for {@link VectorStoreHelper}: vector validation, filter construction and payload
 * accessors.
 */
class VectorStoreHelperTest {

    private static final int DIMENSION = 4;

    private VectorStoreHelper helper;

    @BeforeEach
    void setUp() {

        QdrantProperties properties = new QdrantProperties();
        properties.setCollectionName("test_collection");
        properties.setVectorSize(DIMENSION);
        properties.setScoreThreshold(0.5f);

        helper = new VectorStoreHelper(properties);
    }

    // ===================== vector points =====================

    @Test
    void shouldRejectNullPointList() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateVectorPoints(null)),
                ErrorCode.VECTOR_POINTS_REQUIRED
        );
    }

    @Test
    void shouldAcceptEmptyPointList() {

        assertThat(catchThrowable(() -> helper.validateVectorPoints(List.of()))).isNull();
    }

    @Test
    void shouldRejectNullPointInsideList() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateVectorPoints(java.util.Arrays.asList((VectorPoint) null))),
                ErrorCode.VECTOR_POINT_REQUIRED
        );
    }

    @Test
    void shouldRejectPointWithoutId() {

        VectorPoint point = VectorPoint.builder().vector(new float[DIMENSION]).build();

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateVectorPoints(List.of(point))),
                ErrorCode.VECTOR_POINT_ID_REQUIRED
        );
    }

    @Test
    void shouldRejectMissingOrEmptyVector() {

        VectorPoint noVector = VectorPoint.builder().id(1L).build();
        VectorPoint emptyVector = VectorPoint.builder().id(2L).vector(new float[0]).build();

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateVectorPoints(List.of(noVector))),
                ErrorCode.VECTOR_REQUIRED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateVectorPoints(List.of(emptyVector))),
                ErrorCode.VECTOR_REQUIRED
        );
    }

    @Test
    void shouldRejectWrongVectorDimension() {

        VectorPoint point = VectorPoint.builder().id(3L).vector(new float[DIMENSION - 1]).build();

        Throwable thrown = catchThrowable(() -> helper.validateVectorPoints(List.of(point)));

        ErrorCodeAssertions.assertBusinessError(thrown, ErrorCode.VECTOR_DIMENSION_INVALID);
        assertThat(thrown).hasMessageContaining("Expected 4 but got 3");
    }

    @Test
    void shouldAcceptValidPoints() {

        List<VectorPoint> points = List.of(
                VectorPoint.builder().id(1L).vector(new float[DIMENSION]).build(),
                VectorPoint.builder().id(2L).vector(new float[DIMENSION]).build()
        );

        assertThat(catchThrowable(() -> helper.validateVectorPoints(points))).isNull();
    }

    // ===================== point id / search =====================

    @Test
    void shouldValidatePointId() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validatePointId(null)),
                ErrorCode.VECTOR_POINT_ID_REQUIRED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validatePointId(0L)),
                ErrorCode.VECTOR_POINT_ID_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validatePointId(-1L)),
                ErrorCode.VECTOR_POINT_ID_INVALID
        );

        assertThat(catchThrowable(() -> helper.validatePointId(5L))).isNull();
    }

    @Test
    void shouldRejectInvalidSearchRequests() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(null, 5, null)),
                ErrorCode.VECTOR_REQUIRED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(new float[0], 5, null)),
                ErrorCode.VECTOR_REQUIRED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(new float[DIMENSION - 1], 5, null)),
                ErrorCode.VECTOR_DIMENSION_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(new float[DIMENSION], 0, null)),
                ErrorCode.VECTOR_SEARCH_LIMIT_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(new float[DIMENSION], -3, null)),
                ErrorCode.VECTOR_SEARCH_LIMIT_INVALID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(new float[DIMENSION], 5, 0L)),
                ErrorCode.DOCUMENT_ID_INVALID
        );
    }

    @Test
    void shouldAcceptValidSearchRequest() {

        assertThat(catchThrowable(() -> helper.validateSearchRequest(new float[DIMENSION], 5, 10L)))
                .isNull();
    }

    @Test
    void shouldNotBuildFilterWhenDocumentIdIsAbsent() {

        assertThat(helper.buildDocumentFilter(null)).isNull();
    }

    @Test
    void shouldBuildDocumentIdMatchFilter() {

        Points.Filter filter = helper.buildDocumentFilter(42L);

        assertThat(filter).isNotNull();
        assertThat(filter.getMustCount()).isEqualTo(1);

        Points.Condition condition = filter.getMust(0);
        assertThat(condition.getField().getKey()).isEqualTo("documentId");
        assertThat(condition.getField().getMatch().getInteger()).isEqualTo(42L);
    }

    // ===================== payload =====================

    @Test
    void shouldSkipNullPayloadValues() {

        Map<String, JsonWithInt.Value> payload = new HashMap<>();

        helper.putLong(payload, "chunkId", null);
        helper.putInt(payload, "pageNumber", null);
        helper.putString(payload, "content", null);

        assertThat(payload).isEmpty();

        helper.putLong(payload, "chunkId", 7L);
        helper.putInt(payload, "pageNumber", 3);
        helper.putString(payload, "content", "text");

        assertThat(payload).hasSize(3);
        assertThat(payload.get("chunkId").getIntegerValue()).isEqualTo(7L);
        assertThat(payload.get("pageNumber").getIntegerValue()).isEqualTo(3L);
        assertThat(payload.get("content").getStringValue()).isEqualTo("text");
    }

    @Test
    void shouldReadTypedValuesAndReturnNullWhenAbsentOrMistyped() {

        Map<String, JsonWithInt.Value> payload = new HashMap<>();
        payload.put("chunkId", ValueFactory.value(99L));
        payload.put("content", ValueFactory.value("hello"));

        assertThat(helper.getLong(payload, "chunkId")).isEqualTo(99L);
        assertThat(helper.getInt(payload, "chunkId")).isEqualTo(99);
        assertThat(helper.getString(payload, "content")).isEqualTo("hello");

        assertThat(helper.getLong(payload, "missing")).isNull();
        assertThat(helper.getInt(payload, "content")).isNull();
        assertThat(helper.getString(payload, "chunkId")).isNull();
    }

    @Test
    void shouldExposePayloadBuilderFieldsUnchanged() {

        VectorPayload payload = VectorPayload.builder()
                .chunkId(1L)
                .documentId(2L)
                .documentVersionId(3L)
                .chunkIndex(4)
                .pageNumber(5)
                .startChar(6)
                .endChar(7)
                .tokenCount(8)
                .embeddingModel("model")
                .content("content")
                .build();

        assertThat(payload.getChunkId()).isEqualTo(1L);
        assertThat(payload.getDocumentId()).isEqualTo(2L);
        assertThat(payload.getDocumentVersionId()).isEqualTo(3L);
        assertThat(payload.getChunkIndex()).isEqualTo(4);
        assertThat(payload.getPageNumber()).isEqualTo(5);
        assertThat(payload.getStartChar()).isEqualTo(6);
        assertThat(payload.getEndChar()).isEqualTo(7);
        assertThat(payload.getTokenCount()).isEqualTo(8);
        assertThat(payload.getEmbeddingModel()).isEqualTo("model");
        assertThat(payload.getContent()).isEqualTo("content");
    }
}
