package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link VectorStoreMapper}: payload encoding/decoding, point construction and
 * scored-point mapping.
 */
class VectorStoreMapperTest {

    private static final float[] VECTOR = {0.5f, 0.25f, 0.125f, 0f};

    private VectorStoreMapper mapper;

    @BeforeEach
    void setUp() {

        QdrantProperties properties = new QdrantProperties();
        properties.setVectorSize(VECTOR.length);
        properties.setCollectionName("test_collection");

        mapper = new VectorStoreMapper(new VectorStoreHelper(properties));
    }

    @Test
    void shouldEncodePayloadWithAllFields() {

        Map<String, JsonWithInt.Value> payload = mapper.toQdrantPayload(fullPayload());

        assertThat(payload).containsKeys(
                "chunkId", "documentId", "documentVersionId", "chunkIndex", "pageNumber",
                "startChar", "endChar", "tokenCount", "embeddingModel", "content"
        );
        assertThat(payload.get("chunkId").getIntegerValue()).isEqualTo(11L);
        assertThat(payload.get("documentId").getIntegerValue()).isEqualTo(22L);
        assertThat(payload.get("documentVersionId").getIntegerValue()).isEqualTo(33L);
        assertThat(payload.get("chunkIndex").getIntegerValue()).isEqualTo(4L);
        assertThat(payload.get("pageNumber").getIntegerValue()).isEqualTo(2L);
        assertThat(payload.get("startChar").getIntegerValue()).isEqualTo(100L);
        assertThat(payload.get("endChar").getIntegerValue()).isEqualTo(250L);
        assertThat(payload.get("tokenCount").getIntegerValue()).isEqualTo(37L);
        assertThat(payload.get("embeddingModel").getStringValue()).isEqualTo("gemini-embedding-001");
        assertThat(payload.get("content").getStringValue()).isEqualTo("chunk text");
    }

    @Test
    void shouldRoundTripPayloadThroughQdrantRepresentation() {

        VectorPayload original = fullPayload();

        VectorPayload decoded = mapper.toVectorPayload(mapper.toQdrantPayload(original));

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void shouldOmitNullPayloadFields() {

        Map<String, JsonWithInt.Value> payload = mapper.toQdrantPayload(
                VectorPayload.builder().documentId(1L).content("only").build());

        assertThat(payload).containsOnlyKeys("documentId", "content");
    }

    @Test
    void shouldDecodeMissingKeysAsNull() {

        VectorPayload decoded = mapper.toVectorPayload(Map.of());

        assertThat(decoded.getChunkId()).isNull();
        assertThat(decoded.getDocumentId()).isNull();
        assertThat(decoded.getPageNumber()).isNull();
        assertThat(decoded.getContent()).isNull();
    }

    @Test
    void shouldBuildPointStructWithIdVectorAndPayload() {

        VectorPoint point = VectorPoint.builder()
                .id(77L)
                .vector(VECTOR)
                .payload(fullPayload())
                .build();

        Points.PointStruct struct = mapper.toPointStruct(point);

        assertThat(struct.getId().getNum()).isEqualTo(77L);
        assertThat(struct.getVectors().getVector().getDataList()).containsExactly(0.5f, 0.25f, 0.125f, 0f);
        assertThat(struct.getPayloadMap().get("chunkId").getIntegerValue()).isEqualTo(11L);
    }

    @Test
    void shouldMapScoredPointToSearchResult() {

        Points.ScoredPoint scoredPoint = Points.ScoredPoint.newBuilder()
                .setId(PointIdFactory.id(55L))
                .setScore(0.75f)
                .putAllPayload(mapper.toQdrantPayload(fullPayload()))
                .build();

        SearchResult result = mapper.toSearchResult(scoredPoint);

        assertThat(result.getPointId()).isEqualTo(55L);
        assertThat(result.getScore()).isEqualTo(0.75);
        assertThat(result.getPayload()).isEqualTo(fullPayload());
    }

    @Test
    void shouldMapScoredPointListPreservingOrder() {

        List<SearchResult> results = mapper.toSearchResults(List.of(
                scoredPoint(1L, 0.9f),
                scoredPoint(2L, 0.5f)
        ));

        assertThat(results).extracting(SearchResult::getPointId).containsExactly(1L, 2L);
        assertThat(results.get(0).getScore()).isCloseTo(0.9, within(1e-6));
        assertThat(results.get(1).getScore()).isCloseTo(0.5, within(1e-6));
    }

    @Test
    void shouldConvertFloatArrayToList() {

        assertThat(mapper.toFloatList(VECTOR)).containsExactly(0.5f, 0.25f, 0.125f, 0f);
    }

    private static Points.ScoredPoint scoredPoint(Long id, float score) {

        return Points.ScoredPoint.newBuilder()
                .setId(PointIdFactory.id(id))
                .setScore(score)
                .build();
    }

    private static VectorPayload fullPayload() {

        return VectorPayload.builder()
                .chunkId(11L)
                .documentId(22L)
                .documentVersionId(33L)
                .chunkIndex(4)
                .pageNumber(2)
                .startChar(100)
                .endChar(250)
                .tokenCount(37)
                .embeddingModel("gemini-embedding-001")
                .content("chunk text")
                .build();
    }
}
