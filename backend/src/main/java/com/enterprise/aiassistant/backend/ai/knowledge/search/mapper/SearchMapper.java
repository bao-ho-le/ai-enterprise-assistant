package com.enterprise.aiassistant.backend.ai.knowledge.search.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchMapper {

    private final VectorStoreMapper vectorStoreMapper;

    public SemanticSearchResult toSemanticSearchResult(EmbeddingMatch<TextSegment> match) {

        return SemanticSearchResult.builder()
                .documentId(vectorStoreMapper.documentId(match))
                .versionId(vectorStoreMapper.documentVersionId(match))
                .chunkId(vectorStoreMapper.chunkId(match))
                .score(vectorStoreMapper.cosineScore(match))
                .page(vectorStoreMapper.pageNumber(match))
                .startChar(vectorStoreMapper.startChar(match))
                .endChar(vectorStoreMapper.endChar(match))
                .content(match.embedded().text())
                .build();
    }

    public List<SemanticSearchResult> toSemanticSearchResults(
            List<EmbeddingMatch<TextSegment>> hits
    ) {
        return hits.stream()
                .map(this::toSemanticSearchResult)
                .toList();
    }


}
