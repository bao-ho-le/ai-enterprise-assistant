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
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final DocumentRepository documentRepository;

    private final SemanticSearchValidator validator;

    private final SearchMapper searchMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SemanticSearchResult> search(SemanticSearchRequest request) {

        validator.validate(request);

        int topK = validator.resolveTopK(request.getTopK());

        EmbeddingResult queryEmbedding = embeddingService.embed(request.getKeyword());

        List<SearchResult> hits = vectorStoreService.search(
                queryEmbedding.getVector(),
                topK,
                request.getDocumentId()
        );

        if (hits.isEmpty()) {
            return List.of();
        }

        Map<Long, Document> activeDocumentsById = fetchActiveDocuments(hits);

        // ponytail: only the document's current version is surfaced here; chunks from
        // superseded versions stay indexed in Qdrant until version cleanup is added.
        return hits.stream()
                .filter(hit -> isCurrentVersionHit(hit, activeDocumentsById))
                .map(searchMapper::toSemanticSearchResult)
                .toList();
    }

    private Map<Long, Document> fetchActiveDocuments(List<SearchResult> hits) {

        Set<Long> documentIds = hits.stream()
                .map(hit -> hit.getPayload().getDocumentId())
                .collect(Collectors.toSet());

        return documentRepository.findAllById(documentIds).stream()
                .filter(document -> document.getStatus() == DocumentStatus.ACTIVE)
                .collect(Collectors.toMap(Document::getId, Function.identity()));
    }

    private boolean isCurrentVersionHit(SearchResult hit, Map<Long, Document> activeDocumentsById) {

        VectorPayload payload = hit.getPayload();
        Document document = activeDocumentsById.get(payload.getDocumentId());

        return document != null
                && document.getCurrentVersion() != null
                && document.getCurrentVersion().getId().equals(payload.getDocumentVersionId());
    }

}
