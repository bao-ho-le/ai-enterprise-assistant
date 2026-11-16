package com.enterprise.aiassistant.backend.processing.orchestration.worker;

import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedBatchEvent;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedEvent;
import com.enterprise.aiassistant.backend.processing.orchestration.service.DocumentProcessingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the processing workers and retry executor: invocation count, executor dispatch
 * and exception containment (worker threads must never leak failures).
 */
class ProcessingWorkerTest {

    private final DocumentProcessingRetryExecutor retryExecutor = mock(DocumentProcessingRetryExecutor.class);

    private final DocumentProcessingService processingService = mock(DocumentProcessingService.class);

    @Test
    void shouldDelegateSingleVersionEventToRetryExecutor() {

        new DocumentProcessingWorker(retryExecutor)
                .onDocumentVersionCreated(new DocumentVersionCreatedEvent(42L));

        verify(retryExecutor).processWithRetry(42L);
    }

    @Test
    void shouldSwallowProcessingFailuresInSingleWorker() {

        doThrow(new ProcessingException(ErrorCode.DOCUMENT_PROCESSING_FAILED))
                .when(retryExecutor).processWithRetry(7L);

        assertThatCode(() -> new DocumentProcessingWorker(retryExecutor).submit(7L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldDispatchEachBatchVersionToExecutor() {

        Executor executor = mock(Executor.class);

        new DocumentProcessingBatchWorker(retryExecutor, executor)
                .onDocumentVersionCreated(new DocumentVersionCreatedBatchEvent(List.of(1L, 2L, 3L)));

        verify(executor, org.mockito.Mockito.times(3)).execute(any(Runnable.class));
    }

    @Test
    void shouldProcessVersionInsideDispatchedTaskAndContainFailures() {

        Executor executor = mock(Executor.class);
        doThrow(new ProcessingException(ErrorCode.DOCUMENT_PROCESSING_FAILED))
                .when(retryExecutor).processWithRetry(5L);

        new DocumentProcessingBatchWorker(retryExecutor, executor)
                .onDocumentVersionCreated(new DocumentVersionCreatedBatchEvent(List.of(5L)));

        org.mockito.ArgumentCaptor<Runnable> captor = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(captor.capture());

        assertThatCode(() -> captor.getValue().run()).doesNotThrowAnyException();
        verify(retryExecutor).processWithRetry(5L);
    }

    @Test
    void shouldDelegateRetryExecutorToProcessingService() {

        new DocumentProcessingRetryExecutor(processingService).processWithRetry(11L);

        verify(processingService).process(11L);
    }

    @Test
    void shouldPropagateFailuresFromRetryExecutorToCaller() {

        doThrow(new ProcessingException(ErrorCode.DOCUMENT_PROCESSING_FAILED))
                .when(processingService).process(any());

        assertThatCode(() -> new DocumentProcessingRetryExecutor(processingService).processWithRetry(12L))
                .isInstanceOf(ProcessingException.class);
    }
}
