package com.enterprise.aiassistant.backend.processing.worker;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.event.DocumentVersionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingWorker {

    private final DocumentProcessingRetryExecutor documentProcessingRetryExecutor;

    @Async("documentProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDocumentVersionCreated(DocumentVersionCreatedEvent event) {

        submit(event.versionId());
    }


    public void submit(Long versionId) {
        try {
            documentProcessingRetryExecutor.processWithRetry(versionId);
        } catch (Exception exception) {
            log.error(
                    "{}. versionId={}",
                    ErrorCode.DOCUMENT_PROCESSING_FAILED.getMessage(),
                    versionId,
                    exception
            );
        }
    }
}
