package com.enterprise.aiassistant.backend.ai.search.mapper;

import com.enterprise.aiassistant.backend.ai.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .startChar(payload.getStartChar())
                .endChar(payload.getEndChar())
                .content(payload.getContent())
                .build();
    }

    public List<SemanticSearchResult> toSemanticSearchResults(
            List<SearchResult> hits
    ) {
        return hits.stream()
                .map(this::toSemanticSearchResult)
                .toList();
    }


}
