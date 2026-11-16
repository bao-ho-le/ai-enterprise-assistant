package com.enterprise.aiassistant.backend.ai.knowledge.search.controller;

import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.service.SemanticSearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SemanticSearchController} delegation.
 */
class SemanticSearchControllerTest {

    private final SemanticSearchService semanticSearchService = mock(SemanticSearchService.class);

    private final SemanticSearchController controller =
            new SemanticSearchController(semanticSearchService);

    @Test
    void shouldDelegateSemanticSearch() {

        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword("contract");

        List<SemanticSearchResult> expected = List.of(
                SemanticSearchResult.builder().documentId(1L).content("hit").build());

        when(semanticSearchService.search(request)).thenReturn(expected);

        assertThat(controller.semanticSearch(request)).isSameAs(expected);
        verify(semanticSearchService).search(request);
    }
}
