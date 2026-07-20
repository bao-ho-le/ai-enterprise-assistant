package com.enterprise.aiassistant.backend.ai.vectorstore.helper;

import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.JsonWithInt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// Only place that touches Qdrant's Map<String, Value> payload shape — keeps
// VectorPayload (the business DTO) free of any Qdrant-specific type.
@Component
public class QdrantPayloadConverter {

    public Map<String, JsonWithInt.Value> toQdrantPayload(VectorPayload payload) {
        Map<String, JsonWithInt.Value> map = new HashMap<>();
        putLong(map, "chunkId", payload.getChunkId());
        putLong(map, "documentId", payload.getDocumentId());
        putLong(map, "documentVersionId", payload.getDocumentVersionId());
        putInt(map, "chunkIndex", payload.getChunkIndex());
        putInt(map, "pageNumber", payload.getPageNumber());
        putInt(map, "startChar", payload.getStartChar());
        putInt(map, "endChar", payload.getEndChar());
        putInt(map, "tokenCount", payload.getTokenCount());
        putString(map, "embeddingModel", payload.getEmbeddingModel());
        putString(map, "content", payload.getContent());
        return map;
    }

    public VectorPayload toVectorPayload(Map<String, JsonWithInt.Value> payload) {
        return VectorPayload.builder()
                .chunkId(getLong(payload, "chunkId"))
                .documentId(getLong(payload, "documentId"))
                .documentVersionId(getLong(payload, "documentVersionId"))
                .chunkIndex(getInt(payload, "chunkIndex"))
                .pageNumber(getInt(payload, "pageNumber"))
                .startChar(getInt(payload, "startChar"))
                .endChar(getInt(payload, "endChar"))
                .tokenCount(getInt(payload, "tokenCount"))
                .embeddingModel(getString(payload, "embeddingModel"))
                .content(getString(payload, "content"))
                .build();
    }

    private void putLong(Map<String, JsonWithInt.Value> map, String key, Long value) {
        if (value != null) {
            map.put(key, ValueFactory.value(value));
        }
    }

    private void putInt(Map<String, JsonWithInt.Value> map, String key, Integer value) {
        if (value != null) {
            map.put(key, ValueFactory.value(value.longValue()));
        }
    }

    private void putString(Map<String, JsonWithInt.Value> map, String key, String value) {
        if (value != null) {
            map.put(key, ValueFactory.value(value));
        }
    }

    private Long getLong(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value value = payload.get(key);
        return (value != null && value.hasIntegerValue()) ? value.getIntegerValue() : null;
    }

    private Integer getInt(Map<String, JsonWithInt.Value> payload, String key) {
        Long value = getLong(payload, key);
        return value != null ? value.intValue() : null;
    }

    private String getString(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value value = payload.get(key);
        return (value != null && value.hasStringValue()) ? value.getStringValue() : null;
    }
}
