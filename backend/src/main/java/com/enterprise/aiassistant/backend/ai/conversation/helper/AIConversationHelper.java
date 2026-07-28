package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;

import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class AIConversationHelper {

    private static final int MIN_RECENT_MESSAGES_LIMIT = 1;
    private static final int MAX_RECENT_MESSAGES_LIMIT = 100;

    private static final Set<ConversationType> GENERATION_CONVERSATION_TYPES = EnumSet.of(
            ConversationType.EMAIL_GENERATION,
            ConversationType.REPORT_GENERATION,
            ConversationType.SUMMARY_GENERATION,
            ConversationType.MEETING_MINUTES_GENERATION,
            ConversationType.FORM_GENERATION
    );


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

    public void validateConversationId(Long conversationId) {

        if (conversationId == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_ID_REQUIRED);
        }

        if (conversationId <= 0) {
            throw new AIConversationException(ErrorCode.CONVERSATION_ID_INVALID);
        }
    }

    public void validateRecentMessagesLimit(int recentMessagesLimit) {

        if (recentMessagesLimit < MIN_RECENT_MESSAGES_LIMIT
                || recentMessagesLimit > MAX_RECENT_MESSAGES_LIMIT) {
            throw new AIConversationException(ErrorCode.RECENT_MESSAGES_LIMIT_INVALID);
        }
    }

    public void validateGenerationConversationType(ConversationType conversationType) {

        if (!GENERATION_CONVERSATION_TYPES.contains(conversationType)) {
            throw new AIConversationException(ErrorCode.CONVERSATION_TYPE_NOT_GENERATION);
        }
    }
}
