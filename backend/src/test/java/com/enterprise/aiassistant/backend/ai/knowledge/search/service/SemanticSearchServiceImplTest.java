package com.enterprise.aiassistant.backend.ai.knowledge.search.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.helper.EmbeddingHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.helper.SearchHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.search.mapper.SearchMapper;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceImplTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingHelper embeddingHelper;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private VectorStoreHelper vectorStoreHelper;

    @Spy
    private VectorStoreMapper vectorStoreMapper = new VectorStoreMapper();

    @Mock
    private DocumentRepository documentRepository;

    @Spy
    private SearchHelper searchHelper = new SearchHelper();

    @Spy
    private SearchMapper searchMapper = new SearchMapper(new VectorStoreMapper());

    @Mock
    private DocumentAuthorizationService documentAuthorizationService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SemanticSearchServiceImpl service;

    private final Embedding queryEmbedding = Embedding.from(new float[]{0.1f});

    // Bảo mật: Qdrant không biết phân quyền, nên service phải tự lọc lại trước khi trả kết quả.
    // Chỉ tài liệu 1 hợp lệ; tài liệu 2 đã xóa, tài liệu 3 không có quyền đọc, kết quả của tài liệu 4 thuộc version cũ.
    @Test
    void search_hitsFromInvalidDocuments_returnsOnlyReadableCurrentVersions() {
        Document valid = document(1L, 10L, DocumentStatus.ACTIVE);
        Document deleted = document(2L, 20L, DocumentStatus.DELETED);
        Document unreadable = document(3L, 30L, DocumentStatus.ACTIVE);
        Document newerVersion = document(4L, 41L, DocumentStatus.ACTIVE);
        List<EmbeddingMatch<TextSegment>> hits =
                List.of(hit(1L, 10L, 100L), hit(2L, 20L, 200L), hit(3L, 30L, 300L), hit(4L, 40L, 400L));

        when(embeddingHelper.call(any())).thenReturn(Response.from(queryEmbedding));
        // Chạy lambda truyền vào để embeddingStore.search(...) thật sự được gọi
        when(vectorStoreHelper.call(eq(ErrorCode.VECTOR_SEARCH_FAILED), any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(hits));
        when(documentRepository.findAllById(any())).thenReturn(List.of(valid, deleted, unreadable, newerVersion));
        // Tài liệu đã xóa bị loại trước khi kiểm tra quyền
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(valid, unreadable, newerVersion)))
                .thenReturn(Set.of(1L, 4L));

        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword("hợp đồng");

        assertThat(service.search(request))
                .extracting(SemanticSearchResult::getDocumentId)
                .containsExactly(1L);
    }

    // ===================== Helper =====================

    private Document document(Long id, Long currentVersionId, DocumentStatus status) {
        return Document.builder()
                .id(id)
                .status(status)
                .currentVersion(DocumentVersion.builder().id(currentVersionId).build())
                .build();
    }

    private EmbeddingMatch<TextSegment> hit(Long documentId, Long versionId, Long chunkId) {
        TextSegment segment = TextSegment.from("Đoạn tài liệu", Metadata.from(Map.of(
                VectorStoreMapper.DOCUMENT_ID, documentId,
                VectorStoreMapper.DOCUMENT_VERSION_ID, versionId,
                VectorStoreMapper.CHUNK_ID, chunkId
        )));
        return new EmbeddingMatch<>(0.9, "point-" + chunkId, queryEmbedding, segment);
    }
}
