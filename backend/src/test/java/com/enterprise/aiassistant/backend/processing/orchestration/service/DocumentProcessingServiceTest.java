package com.enterprise.aiassistant.backend.processing.orchestration.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.VectorStoreException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentTextRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.chunking.service.ChunkingService;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import com.enterprise.aiassistant.backend.processing.extraction.helper.ExtractionHelper;
import com.enterprise.aiassistant.backend.processing.extraction.service.TextExtractionService;
import com.enterprise.aiassistant.backend.processing.orchestration.helper.ProcessingHelper;
import com.enterprise.aiassistant.backend.processing.orchestration.mapper.ProcessingMapper;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentProcessingService}: happy-path orchestration, status transitions,
 * failure bookkeeping and usage logging. Collaborators are mocked; the real
 * {@link ProcessingHelper} participates so failure handling is verified end to end.
 */
class DocumentProcessingServiceTest {

    private static final long VERSION_ID = 7L;

    private final DocumentVersionRepository versionRepository = mock(DocumentVersionRepository.class);

    private final FileStorageService fileStorageService = mock(FileStorageService.class);

    private final DocumentTextRepository textRepository = mock(DocumentTextRepository.class);

    private final TextExtractionService extractionService = mock(TextExtractionService.class);

    private final ChunkingService chunkingService = mock(ChunkingService.class);

    private final DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);

    private final EmbeddingService embeddingService = mock(EmbeddingService.class);

    private final VectorStoreService vectorStoreService = mock(VectorStoreService.class);

    private final ProcessingMapper processingMapper = mock(ProcessingMapper.class);

    private final AIUsageLogService aiUsageLogService = mock(AIUsageLogService.class);

    private DocumentProcessingService service;

    private DocumentVersion version;

    @BeforeEach
    void setUp() {

        service = new DocumentProcessingService(
                versionRepository,
                fileStorageService,
                textRepository,
                extractionService,
                new ExtractionHelper(),
                chunkingService,
                chunkRepository,
                embeddingService,
                vectorStoreService,
                processingMapper,
                new ProcessingHelper(versionRepository, aiUsageLogService)
        );

        version = pendingVersion();
    }

    @Test
    void shouldProcessVersionThroughEveryStep() {

        Resource resource = new ByteArrayResource(new byte[]{1, 2});
        ExtractedText extractedText = extractedText();
        List<TextChunk> textChunks = List.of(
                TextChunk.builder().chunkIndex(0).content("chunk-0").pageNumber(1).build(),
                TextChunk.builder().chunkIndex(1).content("chunk-1").pageNumber(1).build()
        );

        DocumentText documentText = new DocumentText();
        DocumentChunk chunk0 = chunk(100L, "chunk-0");
        DocumentChunk chunk1 = chunk(101L, "chunk-1");
        VectorPoint point0 = point(100L);
        VectorPoint point1 = point(101L);

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource("test-files", "key-1")).thenReturn(resource);
        when(extractionService.extract(resource, "text/plain")).thenReturn(extractedText);
        when(processingMapper.toDocumentText(version, extractedText)).thenReturn(documentText);
        when(chunkingService.chunk(extractedText, "txt")).thenReturn(textChunks);
        when(processingMapper.toDocumentChunk(version, textChunks.get(0))).thenReturn(chunk0);
        when(processingMapper.toDocumentChunk(version, textChunks.get(1))).thenReturn(chunk1);
        when(chunkRepository.save(chunk0)).thenReturn(chunk0);
        when(chunkRepository.save(chunk1)).thenReturn(chunk1);
        when(embeddingService.getModelName()).thenReturn("model-x");
        when(embeddingService.embed("chunk-0")).thenReturn(embeddingResult("chunk-0"));
        when(embeddingService.embed("chunk-1")).thenReturn(embeddingResult("chunk-1"));
        when(processingMapper.toVectorPoint(chunk0, embeddingResult("chunk-0"))).thenReturn(point0);
        when(processingMapper.toVectorPoint(chunk1, embeddingResult("chunk-1"))).thenReturn(point1);

        service.process(VERSION_ID);

        assertThat(version.getStatus()).isEqualTo(VersionStatus.READY);
        assertThat(version.getProcessingStep()).isNull();

        verify(textRepository).save(documentText);
        verify(chunkRepository, times(2)).save(any(DocumentChunk.class));

        ArgumentCaptor<List<VectorPoint>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStoreService).upsert(pointsCaptor.capture());
        assertThat(pointsCaptor.getValue()).containsExactly(point0, point1);

        ArgumentCaptor<AIUsageLogRequest> usageCaptor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService, times(2)).logAiUsage(usageCaptor.capture());
        assertThat(usageCaptor.getAllValues())
                .allSatisfy(request -> assertThat(request.getStatus()).isEqualTo(AIUsageStatus.SUCCESS));
    }

    @Test
    void shouldRejectMissingVersion() {

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.empty());

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.process(VERSION_ID)),
                ErrorCode.DOCUMENT_VERSION_NOT_FOUND
        );

        verifyNoInteractions(fileStorageService, chunkingService, vectorStoreService);
    }

    @Test
    void shouldRejectVersionThatIsAlreadyReady() {

        version.setStatus(VersionStatus.READY);
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.process(VERSION_ID)),
                ErrorCode.DOCUMENT_VERSION_INVALID_STATUS
        );

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void shouldMarkVersionFailedWhenExtractedTextIsEmpty() {

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource(any(), any())).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(extractionService.extract(any(), any()))
                .thenReturn(ExtractedText.builder().content("   ").build());

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.process(VERSION_ID)),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );

        assertThat(version.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(version.getProcessingStep()).isEqualTo(ProcessingStep.TEXT_EXTRACTING);
        assertThat(version.getErrorMessage()).isNotBlank();

        verify(textRepository, never()).save(any());
        verifyNoInteractions(vectorStoreService);
    }

    @Test
    void shouldPropagateChunkingFailureAndRecordFailedStep() {

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource(any(), any())).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(extractionService.extract(any(), any())).thenReturn(extractedText());
        when(processingMapper.toDocumentText(any(), any())).thenReturn(new DocumentText());
        when(chunkingService.chunk(any(), any()))
                .thenThrow(new ProcessingException(ErrorCode.DOCUMENT_CHUNKING_FAILED));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.process(VERSION_ID)),
                ErrorCode.DOCUMENT_CHUNKING_FAILED
        );

        assertThat(version.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(version.getProcessingStep()).isEqualTo(ProcessingStep.CHUNKING);
        verifyNoInteractions(embeddingService, vectorStoreService);
    }

    @Test
    void shouldLogFailedUsageAndWrapEmbeddingFailure() {

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource(any(), any())).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(extractionService.extract(any(), any())).thenReturn(extractedText());
        when(processingMapper.toDocumentText(any(), any())).thenReturn(new DocumentText());
        when(chunkingService.chunk(any(), any())).thenReturn(
                List.of(TextChunk.builder().chunkIndex(0).content("chunk").build()));
        when(processingMapper.toDocumentChunk(any(), any())).thenReturn(chunk(1L, "chunk"));
        when(chunkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingService.getModelName()).thenReturn("model-x");
        when(embeddingService.embed(any()))
                .thenThrow(new EmbeddingException(ErrorCode.EMBEDDING_FAILED));

        // EmbeddingException is a BusinessException, so it is rethrown unchanged instead of
        // being re-wrapped as DOCUMENT_PROCESSING_FAILED.
        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.process(VERSION_ID)),
                ErrorCode.EMBEDDING_FAILED
        );

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AIUsageStatus.FAILED);
        assertThat(captor.getValue().getModel()).isEqualTo("model-x");

        assertThat(version.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(version.getProcessingStep()).isEqualTo(ProcessingStep.EMBEDDING);
        verifyNoInteractions(vectorStoreService);
    }

    @Test
    void shouldWrapVectorStoreFailure() {

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource(any(), any())).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(extractionService.extract(any(), any())).thenReturn(extractedText());
        when(processingMapper.toDocumentText(any(), any())).thenReturn(new DocumentText());
        when(chunkingService.chunk(any(), any())).thenReturn(
                List.of(TextChunk.builder().chunkIndex(0).content("chunk").build()));
        when(processingMapper.toDocumentChunk(any(), any())).thenReturn(chunk(2L, "chunk"));
        when(chunkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(embeddingResult("chunk"));
        when(processingMapper.toVectorPoint(any(), any())).thenReturn(point(2L));
        org.mockito.Mockito.doThrow(new VectorStoreException(ErrorCode.VECTOR_UPSERT_FAILED,
                        new RuntimeException("qdrant down")))
                .when(vectorStoreService).upsert(anyList());

        // VectorStoreException is a BusinessException and therefore propagates unchanged.
        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.process(VERSION_ID)),
                ErrorCode.VECTOR_UPSERT_FAILED
        );

        assertThat(version.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(version.getStatus()).isNotEqualTo(VersionStatus.READY);
    }

    // ===================== helpers =====================

    private static DocumentVersion pendingVersion() {

        Document document = Document.builder().id(50L).title("doc").build();

        FileEntity file = FileEntity.builder()
                .bucketName("test-files")
                .objectKey("key-1")
                .mimeType("text/plain")
                .extension("txt")
                .originalFilename("notes.txt")
                .build();

        DocumentVersion documentVersion = DocumentVersion.builder()
                .id(VERSION_ID)
                .document(document)
                .file(file)
                .versionNumber(1)
                .status(VersionStatus.PENDING)
                .build();

        document.setCurrentVersion(documentVersion);

        return documentVersion;
    }

    private static ExtractedText extractedText() {

        return ExtractedText.builder()
                .content("text content")
                .elements(List.of(DocumentElement.builder()
                        .type(ElementType.PARAGRAPH)
                        .content("text content")
                        .pageNumber(1)
                        .build()))
                .build();
    }

    private static DocumentChunk chunk(Long id, String content) {

        DocumentChunk chunk = DocumentChunk.builder()
                .chunkIndex(0)
                .content(content)
                .pageNumber(1)
                .build();
        chunk.setId(id);

        return chunk;
    }

    private static EmbeddingResult embeddingResult(String content) {

        return EmbeddingResult.builder()
                .vector(new float[]{0.1f, 0.2f})
                .dimension(2)
                .model("model-x")
                .inputTokens(content.length())
                .build();
    }

    private static VectorPoint point(Long id) {

        return VectorPoint.builder()
                .id(id)
                .vector(new float[]{0.1f, 0.2f})
                .payload(VectorPayload.builder().chunkId(id).documentId(50L).build())
                .build();
    }
}
