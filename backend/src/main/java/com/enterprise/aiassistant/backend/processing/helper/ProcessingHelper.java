package com.enterprise.aiassistant.backend.processing.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingHelper {

    private final DocumentVersionRepository documentVersionRepository;

    /**
     * Only guards against reprocessing an already-succeeded version. PENDING,
     * PROCESSING and FAILED are all allowed through so that a retry attempt (or a
     * manual re-trigger of a previously failed version) can actually re-run the
     * pipeline instead of instantly bouncing with DOCUMENT_VERSION_INVALID_STATUS
     * — which used to defeat @Retryable entirely, since handleFailed() ran on every
     * attempt and flipped status away from PENDING before the next attempt started.
     */
    public void validateStatus(DocumentVersion version) {

        if(version.getStatus() == VersionStatus.READY){

            throw new BusinessException(
                    ErrorCode.DOCUMENT_VERSION_INVALID_STATUS
            );
        }
    }

    /**
     * Independent transaction so this always commits even though the caller's
     * transaction (DocumentProcessingService#process) is about to roll back.
     * Deliberately never lets an exception escape: this runs from inside an
     * exception-handling path, and it being the ORIGINAL bug (an unhandled
     * exception here previously left versions stuck PENDING with no trace) means
     * it must degrade to a log line rather than fail the whole failure-handling flow.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailed(
            Long versionId,
            Exception exception,
            ProcessingStep failedStep
    ) {
        documentVersionRepository.findById(versionId).ifPresentOrElse(
                version -> {
                    version.setStatus(VersionStatus.FAILED);
                    version.setProcessingStep(failedStep);
                    version.setErrorMessage(exception.getMessage());
                },
                () -> log.warn(
                        "Cannot mark version FAILED, it no longer exists. versionId={}",
                        versionId
                )
        );
    }

}
