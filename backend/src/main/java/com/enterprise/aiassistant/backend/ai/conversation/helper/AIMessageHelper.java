package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import org.springframework.stereotype.Component;

@Component
public class AIMessageHelper {

    public void validateConversationId(Long conversationId) {

        if (conversationId == null) {
            throw new ConversationException(ErrorCode.CONVERSATION_ID_REQUIRED);
        }
    }

    public void validateRequest(SendMessageRequest request) {

        if (request == null) {
            throw new ConversationException(ErrorCode.REQUEST_REQUIRED);
        }

        validateContent(request.getContent());
    }

    public void validateContent(String content) {

        if (content == null || content.isBlank()) {
            throw new ConversationException(ErrorCode.MESSAGE_CONTENT_REQUIRED);
        }

        if (content.length() > 10000) {
            throw new ConversationException(ErrorCode.MESSAGE_CONTENT_TOO_LONG);
        }
    }

    public void validateMessageId(Long messageId) {

        if (messageId == null) {
            throw new ConversationException(ErrorCode.MESSAGE_ID_REQUIRED);
        }
    }
}
