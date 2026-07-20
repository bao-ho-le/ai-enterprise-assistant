package com.enterprise.aiassistant.backend.ai.search.service;

import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.search.mapper.SearchMapper;
import com.enterprise.aiassistant.backend.ai.search.validator.SemanticSearchValidator;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.common.exception.business_exception.SearchException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the one piece of real business logic in this service: a Qdrant hit only
 * surfaces to the caller when it belongs to an ACTIVE document's CURRENT version.
 */
@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceImplTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private VectorStoreService vectorStoreService;
    @Mock private DocumentRepository documentRepository;

    private SemanticSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SemanticSearchServiceImpl(
                embeddingService,
                vectorStoreService,
                documentRepository,
                new SemanticSearchValidator(),
                new SearchMapper()
        );
    }

    private void stubEmbedding(String keyword) {
        when(embeddingService.embed(keyword)).thenReturn(
                EmbeddingResult.builder()
                        .vector(new float[]{0.1f, 0.2f})
                        .dimension(2)
                        .model("gemini-embedding-001")
                        .build()
        );
    }

    private SearchResult hitFor(Long documentId, Long versionId, Long chunkId, double score) {
        return SearchResult.builder()
                .pointId(String.valueOf(chunkId))
                .score(score)
                .payload(VectorPayload.builder()
                        .chunkId(chunkId)
                        .documentId(documentId)
                        .documentVersionId(versionId)
                        .content("some chunk text")
                        .build())
                .build();
    }

    private Document documentWith(Long documentId, DocumentStatus status, Long currentVersionId) {
        return Document.builder()
                .id(documentId)
                .status(status)
                .currentVersion(DocumentVersion.builder().id(currentVersionId).build())
                .build();
    }

    @Test
    void search_blankKeyword_throwsBeforeCallingEmbeddingService() {
        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword(" ");

        assertThrows(SearchException.class, () -> service.search(request));

        verify(embeddingService, never()).embed(any());
    }

    @Test
    void search_noHits_returnsEmptyWithoutQueryingDocuments() {
        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword("machine learning");
        stubEmbedding("machine learning");
        when(vectorStoreService.search(any(), eq(10), eq(null))).thenReturn(List.of());

        List<SemanticSearchResult> results = service.search(request);

        assertTrue(results.isEmpty());
        verify(documentRepository, never()).findAllById(any());
    }

    @Test
    void search_hitFromCurrentVersionOfActiveDocument_isReturned() {
        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword("machine learning");
        stubEmbedding("machine learning");

        SearchResult hit = hitFor(1L, 100L, 500L, 0.92);
        when(vectorStoreService.search(any(), eq(10), eq(null))).thenReturn(List.of(hit));
        when(documentRepository.findAllById(any()))
                .thenReturn(List.of(documentWith(1L, DocumentStatus.ACTIVE, 100L)));

        List<SemanticSearchResult> results = service.search(request);

        assertEquals(1, results.size());
        assertEquals(500L, results.get(0).getChunkId());
        assertEquals(0.92, results.get(0).getScore(), 1e-9);
    }

    @Test
    void search_hitFromStaleVersion_isFilteredOut() {
        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword("machine learning");
        stubEmbedding("machine learning");

        // Document's current version is 100L, but this hit is from an older version (99L).
        SearchResult staleHit = hitFor(1L, 99L, 500L, 0.92);
        when(vectorStoreService.search(any(), eq(10), eq(null))).thenReturn(List.of(staleHit));
        when(documentRepository.findAllById(any()))
                .thenReturn(List.of(documentWith(1L, DocumentStatus.ACTIVE, 100L)));

        assertTrue(service.search(request).isEmpty());
    }

    @Test
    void search_hitFromDeletedDocument_isFilteredOut() {
        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword("machine learning");
        stubEmbedding("machine learning");

        SearchResult hit = hitFor(1L, 100L, 500L, 0.92);
        when(vectorStoreService.search(any(), eq(10), eq(null))).thenReturn(List.of(hit));
        when(documentRepository.findAllById(any()))
                .thenReturn(List.of(documentWith(1L, DocumentStatus.DELETED, 100L)));

        assertTrue(service.search(request).isEmpty());
    }

}
