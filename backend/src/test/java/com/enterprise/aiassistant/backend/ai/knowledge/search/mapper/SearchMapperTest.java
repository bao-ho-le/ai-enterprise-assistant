package com.enterprise.aiassistant.backend.ai.knowledge.search.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchMapper}: vector payload to API response mapping.
 */
class SearchMapperTest {

    private final SearchMapper mapper = new SearchMapper();

    @Test
    void shouldMapEveryPayloadField() {

        SearchResult hit = SearchResult.builder()
                .pointId(1L)
                .score(0.87)
                .payload(VectorPayload.builder()
                        .chunkId(10L)
                        .documentId(20L)
                        .documentVersionId(30L)
                        .chunkIndex(2)
                        .pageNumber(5)
                        .startChar(100)
                        .endChar(200)
                        .content("matched text")
                        .build())
                .build();

        SemanticSearchResult result = mapper.toSemanticSearchResult(hit);

        assertThat(result.getDocumentId()).isEqualTo(20L);
        assertThat(result.getVersionId()).isEqualTo(30L);
        assertThat(result.getChunkId()).isEqualTo(10L);
        assertThat(result.getScore()).isEqualTo(0.87);
        assertThat(result.getPage()).isEqualTo(5);
        assertThat(result.getStartChar()).isEqualTo(100);
        assertThat(result.getEndChar()).isEqualTo(200);
        assertThat(result.getContent()).isEqualTo("matched text");
    }

    @Test
    void shouldPreserveInputOrder() {

        List<SemanticSearchResult> results = mapper.toSemanticSearchResults(List.of(
                hit(1L, 0.9),
                hit(2L, 0.8),
                hit(3L, 0.7)
        ));

        assertThat(results).extracting(SemanticSearchResult::getDocumentId).containsExactly(1L, 2L, 3L);
        assertThat(results).extracting(SemanticSearchResult::getScore).containsExactly(0.9, 0.8, 0.7);
    }

    @Test
    void shouldReturnEmptyListForNoHits() {

        assertThat(mapper.toSemanticSearchResults(List.of())).isEmpty();
    }

    private static SearchResult hit(Long documentId, double score) {

        return SearchResult.builder()
                .pointId(documentId)
                .score(score)
                .payload(VectorPayload.builder().documentId(documentId).build())
                .build();
    }
}
