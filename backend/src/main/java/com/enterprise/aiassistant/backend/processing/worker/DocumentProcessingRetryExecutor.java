package com.enterprise.aiassistant.backend.processing.worker;

import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.VectorStoreException;
import com.enterprise.aiassistant.backend.processing.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentProcessingRetryExecutor {

    private final DocumentProcessingService documentProcessingService;

    // Only transient failures (storage, extraction/chunking, embedding, vector store)
    // are retried. Business-rule failures (BusinessException not of these types) fail fast.
    @Retryable(
            retryFor = {
                    FileStorageException.class,
                    ProcessingException.class,
                    EmbeddingException.class,
                    VectorStoreException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void processWithRetry(Long versionId) {
        documentProcessingService.process(versionId);
    }
}
