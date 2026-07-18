package com.enterprise.aiassistant.backend.processing.worker;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingWorker {

    private final DocumentProcessingService documentProcessingService;

    @Retryable(
            retryFor = {
                    FileStorageException.class,
                    ProcessingException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    @Async("documentProcessingExecutor")
    public void submit(Long versionId) {

        documentProcessingService.process(versionId);
    }

    @Recover
    public void recover(Exception exception, Long versionId) {

        log.error(
                "{}. versionId={}",
                ErrorCode.DOCUMENT_PROCESSING_FAILED.getMessage(),
                versionId,
                exception
        );
    }
}