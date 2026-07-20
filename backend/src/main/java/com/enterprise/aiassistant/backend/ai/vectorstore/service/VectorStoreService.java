package com.enterprise.aiassistant.backend.ai.vectorstore.service;

import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPoint;

import java.util.List;

public interface VectorStoreService {

    void upsert(VectorPoint point);

    void upsert(List<VectorPoint> points);

    void delete(String pointId);

    default List<SearchResult> search(float[] queryVector, int limit) {
        return search(queryVector, limit, null);
    }

    // documentId narrows the search to a single document's chunks; null searches all.
    List<SearchResult> search(
            float[] queryVector,
            int limit,
            Long documentId
    );

}