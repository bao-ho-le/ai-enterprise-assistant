package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
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

    public void validateConversationId(Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
    }

    public void validateCreateRequest(CreateConversationRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()
                || request.getConversationType() == null) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    public void validateRenameRequest(Long conversationId, RenameConversationRequest request) {
        validateConversationId(conversationId);
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    public void validateAttachRequest(Long conversationId, AttachDocumentsRequest request) {
        validateConversationId(conversationId);
        if (request == null || request.getDocumentVersionIds() == null || request.getDocumentVersionIds().isEmpty()) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }
}
