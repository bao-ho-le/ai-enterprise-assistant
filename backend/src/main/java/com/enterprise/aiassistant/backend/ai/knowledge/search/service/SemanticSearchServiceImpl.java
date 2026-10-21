package com.enterprise.aiassistant.backend.ai.knowledge.search.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.helper.EmbeddingHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.helper.SearchHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.mapper.SearchMapper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingModel embeddingModel;

    private final EmbeddingHelper embeddingHelper;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final VectorStoreHelper vectorStoreHelper;

    private final VectorStoreMapper vectorStoreMapper;

    private final DocumentRepository documentRepository;

    private final SearchHelper searchHelper;

    private final SearchMapper searchMapper;

    private final DocumentAuthorizationService documentAuthorizationService;

    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<SemanticSearchResult> search(SemanticSearchRequest request) {

        searchHelper.validateSearchRequest(request);

        currentUserService.requirePermission(Permission.AI_SEMANTIC_SEARCH);

        int topK = searchHelper.resolveTopK(request.getTopK());

        String model = embeddingHelper.getModelName();
        Integer inputTokens = null;

        try {
            Response<Embedding> queryEmbedding = embeddingHelper.call(() -> embeddingModel.embed(request.getKeyword()));
            inputTokens = embeddingHelper.inputTokens(queryEmbedding, List.of(request.getKeyword()));

            // Nếu có documentId thì chỉ search trong document đó, không thì search toàn bộ Qdrant
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding.content())
                    .maxResults(topK)
                    .minScore(vectorStoreHelper.minScore())
                    .filter(request.getDocumentId() != null
                            ? metadataKey(VectorStoreMapper.DOCUMENT_ID).isEqualTo(request.getDocumentId())
                            : null)
                    .build();

            List<EmbeddingMatch<TextSegment>> hits = vectorStoreHelper.call(
                    ErrorCode.VECTOR_SEARCH_FAILED,
                    () -> embeddingStore.search(searchRequest).matches()
            );

            List<SemanticSearchResult> results;
            if (hits.isEmpty()) {
                results = List.of();
            } else {
                Map<Long, Document> activeDocumentsById = fetchActiveDocuments(hits);

                List<EmbeddingMatch<TextSegment>> validHits = hits.stream()
                        .filter(hit -> isCurrentVersionHit(hit, activeDocumentsById))
                        .toList();

                results = searchMapper.toSemanticSearchResults(validHits);
            }

            searchHelper.logUsage(model, inputTokens, AIUsageStatus.SUCCESS, null);
            return results;

        } catch (RuntimeException ex) {
            searchHelper.logUsage(model, inputTokens, AIUsageStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }


    // Helper

    // Chỉ lấy document còn ACTIVE và user hiện tại có quyền đọc — Qdrant không tự biết
    // phân quyền nên phải lọc lại trước khi trả kết quả ra ngoài.
    private Map<Long, Document> fetchActiveDocuments(List<EmbeddingMatch<TextSegment>> hits) {

        Set<Long> documentIds = hits.stream()
                .map(vectorStoreMapper::documentId)
                .collect(Collectors.toSet());

        List<Document> activeDocuments = documentRepository.findAllById(documentIds).stream()
                .filter(document -> document.getStatus() == DocumentStatus.ACTIVE)
                .toList();

        Set<Long> readableDocumentIds =
                documentAuthorizationService.filterReadableDocumentIds(activeDocuments);

        return activeDocuments.stream()
                .filter(document -> readableDocumentIds.contains(document.getId()))
                .collect(Collectors.toMap(Document::getId, Function.identity()));
    }

    // Kiểm tra nếu current version giống với bản trong request thì mới cho search
    // Tránh lấy hits từ version cũ
    private boolean isCurrentVersionHit(EmbeddingMatch<TextSegment> hit, Map<Long, Document> activeDocumentsById) {

        Document document = activeDocumentsById.get(vectorStoreMapper.documentId(hit));

        return document != null
                && document.getCurrentVersion() != null
                && document.getCurrentVersion().getId().equals(vectorStoreMapper.documentVersionId(hit));
    }

}
