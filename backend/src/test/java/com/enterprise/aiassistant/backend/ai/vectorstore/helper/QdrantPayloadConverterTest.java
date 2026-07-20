package com.enterprise.aiassistant.backend.ai.vectorstore.helper;

import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import io.qdrant.client.grpc.JsonWithInt;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class QdrantPayloadConverterTest {

    private final QdrantPayloadConverter converter = new QdrantPayloadConverter();

    @Test
    void toQdrantPayload_thenBack_roundTripsAllFields() {
        VectorPayload original = VectorPayload.builder()
                .chunkId(1L)
                .documentId(2L)
                .documentVersionId(3L)
                .chunkIndex(4)
                .pageNumber(5)
                .startChar(0)
                .endChar(1000)
                .tokenCount(250)
                .embeddingModel("gemini-embedding-001")
                .content("hello world")
                .build();

        Map<String, JsonWithInt.Value> qdrantPayload = converter.toQdrantPayload(original);
        VectorPayload roundTripped = converter.toVectorPayload(qdrantPayload);

        assertEquals(original, roundTripped);
    }

    @Test
    void toQdrantPayload_nullPageNumber_omitsKeyEntirely() {
        VectorPayload payload = VectorPayload.builder()
                .chunkId(1L)
                .documentId(2L)
                .documentVersionId(3L)
                .chunkIndex(0)
                .pageNumber(null)
                .content("hello")
                .build();

        Map<String, JsonWithInt.Value> qdrantPayload = converter.toQdrantPayload(payload);

        assertFalse(qdrantPayload.containsKey("pageNumber"));
        assertNull(converter.toVectorPayload(qdrantPayload).getPageNumber());
    }

}
