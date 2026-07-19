package com.enterprise.aiassistant.backend.processing.worker;

import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Holds the {@code @Retryable} boundary as its own bean, deliberately separate
 * from {@link DocumentProcessingWorker}. Spring's retry/async advice is proxy-based,
 * so a method calling another {@code @Retryable} method on the SAME bean (self-invocation)
 * bypasses the proxy entirely and silently disables retry. Keeping this on a distinct
 * bean guarantees the proxy is always applied, regardless of advisor ordering.
 */
@Component
@RequiredArgsConstructor
public class DocumentProcessingRetryExecutor {

    private final DocumentProcessingService documentProcessingService;

    // Only transient failures (storage I/O, extraction/chunking) are retried.
    // Business-rule failures (BusinessException not of these types) fail fast.
    @Retryable(
            retryFor = {
                    FileStorageException.class,
                    ProcessingException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void processWithRetry(Long versionId) {
        documentProcessingService.process(versionId);
    }
}
