package com.enterprise.aiassistant.backend.ai.search.dto.request;

import lombok.Data;

@Data
public class SemanticSearchRequest {

    private String keyword;

    private Integer topK;

    // Narrows the search to a single document; omit to search across all documents.
    private Long documentId;

}
