package com.enterprise.aiassistant.backend.ai.conversation.helper;

import org.springframework.stereotype.Component;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;

@Component
public class AIConversationHelper {

    private static final int MIN_RECENT_MESSAGES_LIMIT = 1;
    private static final int MAX_RECENT_MESSAGES_LIMIT = 100;

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
}
