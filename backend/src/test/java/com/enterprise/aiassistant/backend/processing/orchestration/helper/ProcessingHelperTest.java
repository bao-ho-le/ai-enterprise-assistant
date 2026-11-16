package com.enterprise.aiassistant.backend.processing.orchestration.helper;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProcessingHelper}: status guard, FAILED bookkeeping and usage logging.
 */
class ProcessingHelperTest {

    private final DocumentVersionRepository versionRepository = mock(DocumentVersionRepository.class);

    private final AIUsageLogService aiUsageLogService = mock(AIUsageLogService.class);

    private ProcessingHelper helper;

    @BeforeEach
    void setUp() {
        helper = new ProcessingHelper(versionRepository, aiUsageLogService);
    }

    @Test
    void shouldRejectVersionThatIsAlreadyReady() {

        DocumentVersion ready = DocumentVersion.builder().id(1L).status(VersionStatus.READY).build();

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateStatus(ready)),
                ErrorCode.DOCUMENT_VERSION_INVALID_STATUS
        );
    }

    @Test
    void shouldAcceptPendingProcessingAndFailedStatuses() {

        assertThat(catchThrowable(() -> helper.validateStatus(
                DocumentVersion.builder().status(VersionStatus.PENDING).build()))).isNull();
        assertThat(catchThrowable(() -> helper.validateStatus(
                DocumentVersion.builder().status(VersionStatus.PROCESSING).build()))).isNull();
        assertThat(catchThrowable(() -> helper.validateStatus(
                DocumentVersion.builder().status(VersionStatus.FAILED).build()))).isNull();
    }

    @Test
    void shouldMarkExistingVersionFailedWithStepAndMessage() {

        DocumentVersion version = DocumentVersion.builder().id(7L).status(VersionStatus.PROCESSING).build();
        when(versionRepository.findById(7L)).thenReturn(Optional.of(version));

        helper.handleFailed(7L, new IllegalStateException("extraction boom"), ProcessingStep.TEXT_EXTRACTING);

        assertThat(version.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(version.getProcessingStep()).isEqualTo(ProcessingStep.TEXT_EXTRACTING);
        assertThat(version.getErrorMessage()).isEqualTo("extraction boom");
    }

    @Test
    void shouldIgnoreFailureForVersionThatNoLongerExists() {

        when(versionRepository.findById(8L)).thenReturn(Optional.empty());

        helper.handleFailed(8L, new IllegalStateException("gone"), ProcessingStep.CHUNKING);

        verify(versionRepository).findById(8L);
    }

    @Test
    void shouldNotOverwriteSuccessfulProcessing() {

        DocumentVersion ready = DocumentVersion.builder().id(9L).status(VersionStatus.READY).build();

        helper.handleFailureUnlessAlreadySucceeded(9L, ready, new IllegalStateException("late"));

        verify(versionRepository, never()).findById(9L);
        assertThat(ready.getStatus()).isEqualTo(VersionStatus.READY);
    }

    @Test
    void shouldMarkFailedWhenVersionHasNotSucceeded() {

        DocumentVersion processing = DocumentVersion.builder()
                .id(10L)
                .status(VersionStatus.PROCESSING)
                .processingStep(ProcessingStep.EMBEDDING)
                .build();

        when(versionRepository.findById(10L)).thenReturn(Optional.of(processing));

        helper.handleFailureUnlessAlreadySucceeded(10L, processing, new IllegalStateException("boom"));

        assertThat(processing.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(processing.getProcessingStep()).isEqualTo(ProcessingStep.EMBEDDING);
    }

    @Test
    void shouldMarkFailedWithNullStepWhenVersionIsUnknown() {

        DocumentVersion reloaded = DocumentVersion.builder().id(11L).status(VersionStatus.PROCESSING).build();
        when(versionRepository.findById(11L)).thenReturn(Optional.of(reloaded));

        helper.handleFailureUnlessAlreadySucceeded(11L, null, new IllegalStateException("missing"));

        assertThat(reloaded.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(reloaded.getProcessingStep()).isNull();
    }

    @Test
    void shouldLogDocumentIndexingUsage() {

        helper.logUsage("gemini-embedding-001", 42, AIUsageStatus.SUCCESS, null);

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());

        AIUsageLogRequest request = captor.getValue();

        assertThat(request.getConversationType()).isEqualTo(ConversationType.DOCUMENT_INDEXING);
        assertThat(request.getModel()).isEqualTo("gemini-embedding-001");
        assertThat(request.getInputTokens()).isEqualTo(42);
        assertThat(request.getOutputTokens()).isZero();
        assertThat(request.getStatus()).isEqualTo(AIUsageStatus.SUCCESS);
    }
}
