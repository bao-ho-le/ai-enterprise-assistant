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

    /**
     * Fires only AFTER the publishing transaction (DocumentServiceImpl#upload /
     * #uploadNewVersion) has committed — never before. Running this any earlier
     * let the async thread race the DB commit: it could start processing before
     * the DocumentVersion row was even visible, throw DOCUMENT_VERSION_NOT_FOUND
     * before any try/catch could handle it, and leave the version stuck PENDING
     * forever with no retry (that exception isn't a retryable type).
     * fallbackExecution=true keeps this robust even if ever published outside a
     * transaction (default behavior would silently drop the event instead).
     */
    @Async("documentProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDocumentVersionCreated(DocumentVersionCreatedEvent event) {
        submit(event.versionId());
    }

    /**
     * Entry point for (re)triggering processing of a version. Safe to call directly
     * (e.g. a future manual "retry failed document" action) since it does not depend
     * on being inside a transaction.
     *
     * Terminal DB state (READY/FAILED) is always the responsibility of
     * DocumentProcessingService/ProcessingHelper, not this method — by the time an
     * exception reaches here, the version has already been marked FAILED (or, in the
     * one safe edge case, left untouched because it was already READY). This catch
     * exists purely so a fully-exhausted retry or a non-retryable failure doesn't
     * disappear into the default async exception handler with no trace.
     */
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
