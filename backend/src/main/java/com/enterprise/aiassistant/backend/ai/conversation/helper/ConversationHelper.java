package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationHelper {

    public List<DocumentVersion> filterNewVersions(
            List<DocumentVersion> versions,
            List<Long> alreadyAttachedVersionIds
    ) {
        return versions.stream()
                .filter(version -> !alreadyAttachedVersionIds.contains(version.getId()))
                .toList();
    }

    public void validateCreateConversationRequest(CreateConversationRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()
                || request.getConversationType() == null) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }
}
