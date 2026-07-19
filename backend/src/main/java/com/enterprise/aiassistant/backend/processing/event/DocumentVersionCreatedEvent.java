package com.enterprise.aiassistant.backend.processing.event;

/**
 * Published (within the upload transaction) whenever a new DocumentVersion is
 * created and needs async processing. Consumed only after the transaction
 * commits — see DocumentProcessingWorker#onDocumentVersionCreated.
 */
public record DocumentVersionCreatedEvent(Long versionId) {
}
