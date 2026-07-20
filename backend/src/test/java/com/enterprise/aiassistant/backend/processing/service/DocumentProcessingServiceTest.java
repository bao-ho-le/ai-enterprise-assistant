package com.enterprise.aiassistant.backend.processing.service;

import com.enterprise.aiassistant.backend.ai.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.vectorstore.service.VectorStoreService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentTextRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.helper.ProcessingHelper;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression coverage for the "DocumentVersion stuck at PENDING" bug: every
 * failure path in process() must reach ProcessingHelper#handleFailed exactly
 * once — using the raw versionId (no NPE when the version wasn't even found) —
 * and a version that already reached READY must never be overwritten back to
 * FAILED by a stray duplicate trigger.
 */
@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock private DocumentVersionRepository documentVersionRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentTextRepository documentTextRepository;
    @Mock private TextExtractionService textExtractionService;
    @Mock private ChunkingService chunkingService;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private VectorStoreService vectorStoreService;
    @Mock private ProcessingHelper processingHelper;

    private DocumentProcessingService service;

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingService(
                documentVersionRepository,
                fileStorageService,
                documentTextRepository,
                textExtractionService,
                chunkingService,
                documentChunkRepository,
                embeddingService,
                vectorStoreService,
                new ProcessingMapper(),
                processingHelper
        );
    }

    private DocumentVersion versionWith(VersionStatus status) {
        FileEntity file = FileEntity.builder()
                .bucketName("documents")
                .objectKey("key")
                .mimeType("application/pdf")
                .build();
        return DocumentVersion.builder()
                .id(42L)
                .status(status)
                .file(file)
                .build();
    }

    @Test
    void process_success_marksReadyAndNeverCallsHandleFailed() {
        DocumentVersion version = versionWith(VersionStatus.PENDING);
        when(documentVersionRepository.findById(42L)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource("documents", "key"))
                .thenReturn(new ByteArrayResource("hi".getBytes()));
        when(textExtractionService.extract(any(), eq("application/pdf")))
                .thenReturn(ExtractedText.builder().content("hello world").build());
        when(chunkingService.chunk("hello world")).thenReturn(List.of());

        service.process(42L);

        assertEquals(VersionStatus.READY, version.getStatus());
        assertNull(version.getProcessingStep());
        verify(processingHelper, never()).handleFailed(anyLong(), any(), any());
    }

    @Test
    void process_versionNotFound_marksFailedWithRawIdAndDoesNotNpe() {
        when(documentVersionRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.process(99L));

        assertEquals(ErrorCode.DOCUMENT_VERSION_NOT_FOUND, ex.getErrorCode());
        verify(processingHelper).handleFailed(eq(99L), any(), isNull());
    }

    @Test
    void process_extractionFails_marksFailedAndPreservesExceptionTypeForRetry() {
        DocumentVersion version = versionWith(VersionStatus.PENDING);
        when(documentVersionRepository.findById(42L)).thenReturn(Optional.of(version));
        when(fileStorageService.loadAsResource("documents", "key"))
                .thenReturn(new ByteArrayResource("hi".getBytes()));

        ProcessingException extractionFailure = new ProcessingException(
                ErrorCode.TEXT_EXTRACTION_FAILED,
                ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                new IOException("corrupt file")
        );
        when(textExtractionService.extract(any(), eq("application/pdf")))
                .thenThrow(extractionFailure);

        BusinessException thrown = assertThrows(BusinessException.class, () -> service.process(42L));

        // Must be the SAME exception, not wrapped — @Retryable's retryFor list
        // matches on this exact type; wrapping would silently disable retry.
        assertSame(extractionFailure, thrown);
        verify(processingHelper).handleFailed(eq(42L), same(extractionFailure), eq(ProcessingStep.TEXT_EXTRACTING));
    }

    @Test
    void process_alreadyReady_doesNotOverwriteWithFailed() {
        DocumentVersion version = versionWith(VersionStatus.READY);
        when(documentVersionRepository.findById(42L)).thenReturn(Optional.of(version));
        doThrow(new BusinessException(ErrorCode.DOCUMENT_VERSION_INVALID_STATUS))
                .when(processingHelper).validateStatus(version);

        assertThrows(BusinessException.class, () -> service.process(42L));

        verify(processingHelper, never()).handleFailed(anyLong(), any(), any());
    }
}
