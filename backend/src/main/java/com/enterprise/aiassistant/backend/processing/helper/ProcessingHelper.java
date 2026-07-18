package com.enterprise.aiassistant.backend.processing.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcessingHelper {

    private final DocumentVersionRepository documentVersionRepository;

    public void validateStatus(DocumentVersion version) {

        if(version.getStatus() != VersionStatus.PENDING){

            throw new BusinessException(
                    ErrorCode.DOCUMENT_VERSION_INVALID_STATUS
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailed(
            Long versionId,
            Exception exception,
            ProcessingStep failedStep
    ) {
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));

        version.setStatus(VersionStatus.FAILED);
        version.setProcessingStep(failedStep);
        version.setErrorMessage(exception.getMessage());
    }

}
