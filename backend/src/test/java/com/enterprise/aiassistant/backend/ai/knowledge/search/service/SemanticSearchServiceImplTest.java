package com.enterprise.aiassistant.backend.ai.knowledge.search.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.helper.SearchHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.mapper.SearchMapper;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.VectorStoreException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SemanticSearchServiceImpl}: validation, authorization, hit filtering
 * (deleted document, stale version, unreadable document) and usage logging.
 */
class SemanticSearchServiceImplTest {

    private static final float[] QUERY_VECTOR = {0.1f, 0.2f, 0.3f, 0.4f};

    private static final String MODEL = "gemini-embedding-001";

    private final EmbeddingService embeddingService = mock(EmbeddingService.class);

    private final VectorStoreService vectorStoreService = mock(VectorStoreService.class);

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);

    private final AIUsageLogService aiUsageLogService = mock(AIUsageLogService.class);

    private final DocumentAuthorizationService documentAuthorizationService =
            mock(DocumentAuthorizationService.class);

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private final SearchHelper searchHelper = new SearchHelper(aiUsageLogService);

    private SemanticSearchServiceImpl service;

    @BeforeEach
    void setUp() {

        service = new SemanticSearchServiceImpl(
                embeddingService,
                vectorStoreService,
                documentRepository,
                searchHelper,
                new SearchMapper(),
                documentAuthorizationService,
                currentUserService
        );

        when(embeddingService.getModelName()).thenReturn(MODEL);
        when(embeddingService.embed(any())).thenReturn(embeddingResult());
    }

    @Test
    void shouldRejectInvalidRequestBeforeEmbedding() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(request("  ", null, null))),
                ErrorCode.SEARCH_KEYWORD_REQUIRED
        );

        verifyNoInteractions(embeddingService, vectorStoreService);
    }

    @Test
    void shouldRequireSemanticSearchPermission() {

        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(Permission.AI_SEMANTIC_SEARCH);

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(request("contract", null, null))),
                ErrorCode.PERMISSION_DENIED
        );

        verifyNoInteractions(embeddingService, vectorStoreService);
    }

    @Test
    void shouldReturnEmptyResultsAndLogUsageWhenVectorStoreHasNoHits() {

        when(vectorStoreService.search(QUERY_VECTOR, 10, null)).thenReturn(List.of());

        assertThat(service.search(request("contract", null, null))).isEmpty();

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AIUsageStatus.SUCCESS);
        assertThat(captor.getValue().getModel()).isEqualTo(MODEL);
        assertThat(captor.getValue().getInputTokens()).isEqualTo(7);
    }

    @Test
    void shouldMapCurrentVersionHitForReadableActiveDocument() {

        Document document = readableDocument(100L, 200L, DocumentStatus.ACTIVE);

        when(vectorStoreService.search(QUERY_VECTOR, 5, null)).thenReturn(List.of(hit(100L, 200L)));
        when(documentRepository.findAllById(Set.of(100L))).thenReturn(List.of(document));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document)))
                .thenReturn(Set.of(100L));

        List<SemanticSearchResult> results = service.search(request("contract", 5, null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocumentId()).isEqualTo(100L);
        assertThat(results.get(0).getVersionId()).isEqualTo(200L);
        assertThat(results.get(0).getContent()).isEqualTo("chunk content");
    }

    @Test
    void shouldFilterHitsFromDeletedDocuments() {

        Document deleted = readableDocument(101L, 201L, DocumentStatus.DELETED);

        when(vectorStoreService.search(anyFloatArray(), anyInt(), any())).thenReturn(List.of(hit(101L, 201L)));
        when(documentRepository.findAllById(Set.of(101L))).thenReturn(List.of(deleted));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of()))
                .thenReturn(Set.of());

        assertThat(service.search(request("contract", 5, null))).isEmpty();
    }

    @Test
    void shouldFilterHitsFromPreviousVersions() {

        Document document = readableDocument(102L, 999L, DocumentStatus.ACTIVE);

        when(vectorStoreService.search(anyFloatArray(), anyInt(), any())).thenReturn(List.of(hit(102L, 202L)));
        when(documentRepository.findAllById(Set.of(102L))).thenReturn(List.of(document));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document)))
                .thenReturn(Set.of(102L));

        assertThat(service.search(request("contract", 5, null))).isEmpty();
    }

    @Test
    void shouldFilterHitsFromDocumentsTheUserCannotRead() {

        Document document = readableDocument(103L, 203L, DocumentStatus.ACTIVE);

        when(vectorStoreService.search(anyFloatArray(), anyInt(), any())).thenReturn(List.of(hit(103L, 203L)));
        when(documentRepository.findAllById(Set.of(103L))).thenReturn(List.of(document));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document)))
                .thenReturn(Set.of());

        assertThat(service.search(request("contract", 5, null))).isEmpty();
    }

    @Test
    void shouldFilterHitsWhenDocumentHasNoCurrentVersion() {

        Document document = Document.builder().id(104L).status(DocumentStatus.ACTIVE).build();

        when(vectorStoreService.search(anyFloatArray(), anyInt(), any())).thenReturn(List.of(hit(104L, 204L)));
        when(documentRepository.findAllById(Set.of(104L))).thenReturn(List.of(document));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document)))
                .thenReturn(Set.of(104L));

        assertThat(service.search(request("contract", 5, null))).isEmpty();
    }

    @Test
    void shouldForwardDocumentIdFilterToVectorStore() {

        when(vectorStoreService.search(QUERY_VECTOR, 10, 55L)).thenReturn(List.of());

        service.search(request("contract", null, 55L));

        verify(vectorStoreService).search(QUERY_VECTOR, 10, 55L);
    }

    @Test
    void shouldLogFailureAndRethrowWhenEmbeddingFails() {

        when(embeddingService.embed("contract"))
                .thenThrow(new com.enterprise.aiassistant.backend.common.exception.business_exception
                        .EmbeddingException(ErrorCode.EMBEDDING_FAILED));

        Throwable thrown = catchThrowable(() -> service.search(request("contract", null, null)));

        ErrorCodeAssertions.assertBusinessError(thrown, ErrorCode.EMBEDDING_FAILED);

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AIUsageStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).isNotBlank();
        assertThat(captor.getValue().getInputTokens()).isNull();

        verifyNoInteractions(vectorStoreService);
    }

    @Test
    void shouldLogFailureAndRethrowWhenVectorSearchFails() {

        when(vectorStoreService.search(anyFloatArray(), anyInt(), any()))
                .thenThrow(new VectorStoreException(ErrorCode.VECTOR_SEARCH_FAILED, new RuntimeException("boom")));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.search(request("contract", null, null))),
                ErrorCode.VECTOR_SEARCH_FAILED
        );

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AIUsageStatus.FAILED);
        assertThat(captor.getValue().getInputTokens()).isEqualTo(7);
    }

    @Test
    void shouldSkipDocumentLookupWhenThereAreNoHits() {

        when(vectorStoreService.search(anyFloatArray(), anyInt(), any())).thenReturn(List.of());

        service.search(request("contract", null, null));

        verify(documentRepository, never()).findAllById(anyList());
        verifyNoInteractions(documentAuthorizationService);
    }

    @Test
    void shouldNotPassDocumentIdWhenRequestOmitsIt() {

        when(vectorStoreService.search(anyFloatArray(), anyInt(), isNull())).thenReturn(List.of());

        service.search(request("contract", 3, null));

        verify(vectorStoreService).search(QUERY_VECTOR, 3, null);
        verify(vectorStoreService, never()).search(any(), anyInt(), anyLong());
    }

    // ===================== helpers =====================

    private static float[] anyFloatArray() {
        return org.mockito.ArgumentMatchers.any(float[].class);
    }

    private static EmbeddingResult embeddingResult() {

        return EmbeddingResult.builder()
                .vector(QUERY_VECTOR)
                .dimension(QUERY_VECTOR.length)
                .model(MODEL)
                .inputTokens(7)
                .build();
    }

    private static SemanticSearchRequest request(String keyword, Integer topK, Long documentId) {

        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword(keyword);
        request.setTopK(topK);
        request.setDocumentId(documentId);

        return request;
    }

    private static Document readableDocument(Long id, Long currentVersionId, DocumentStatus status) {

        Document document = Document.builder()
                .id(id)
                .title("doc-" + id)
                .status(status)
                .build();

        document.setCurrentVersion(DocumentVersion.builder().id(currentVersionId).build());

        return document;
    }

    private static SearchResult hit(Long documentId, Long versionId) {

        return SearchResult.builder()
                .pointId(1L)
                .score(0.9)
                .payload(VectorPayload.builder()
                        .chunkId(9L)
                        .documentId(documentId)
                        .documentVersionId(versionId)
                        .pageNumber(1)
                        .startChar(0)
                        .endChar(10)
                        .content("chunk content")
                        .build())
                .build();
    }
}
