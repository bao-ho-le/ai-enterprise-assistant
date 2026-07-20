package com.enterprise.aiassistant.backend.ai.search.mapper;

import com.enterprise.aiassistant.backend.ai.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import org.springframework.stereotype.Component;

@Component
public class SearchMapper {

    public SemanticSearchResult toSemanticSearchResult(SearchResult searchResult) {

        VectorPayload payload = searchResult.getPayload();

        return SemanticSearchResult.builder()
                .documentId(payload.getDocumentId())
                .versionId(payload.getDocumentVersionId())
                .chunkId(payload.getChunkId())
                .score(searchResult.getScore())
                .page(payload.getPageNumber())
                .content(payload.getContent())
                .build();
    }

}
