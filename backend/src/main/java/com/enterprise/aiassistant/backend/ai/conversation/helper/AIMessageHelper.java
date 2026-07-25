package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AIMessageHelper {

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository aiMessageRepository;

    /**
     * Dùng cho:
     * - AIMessageService.sendMessage()
     */
    public void validateConversationId(Long conversationId) {

        if (conversationId == null) {
            throw new ConversationException(
                    ErrorCode.CONVERSATION_ID_REQUIRED
            );
        }
    }

    /**
     * Dùng cho:
     * - AIMessageService.sendMessage()
     */
    public void validateRequest(SendMessageRequest request) {

        if (request == null) {
            throw new ConversationException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        validateContent(request.getContent());
    }

    /**
     * Dùng cho:
     * - AIMessageService.sendMessage()
     */
    public void validateContent(String content) {

        if (content == null || content.isBlank()) {
            throw new ConversationException(
                    ErrorCode.MESSAGE_CONTENT_REQUIRED
            );
        }

        if (content.length() > 10000) {
            throw new ConversationException(
                    ErrorCode.MESSAGE_CONTENT_TOO_LONG
            );
        }
    }

    public void validateMessageId(Long messageId) {

        if (messageId == null) {
            throw new ConversationException(
                    ErrorCode.MESSAGE_ID_REQUIRED
            );
        }
    }

    /**
     * Dùng cho:
     * - AIMessageService.sendMessage()
     */
    public AIConversation getConversationOrThrow(Long conversationId) {

        validateConversationId(conversationId);

        return conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new ConversationException(
                                ErrorCode.CONVERSATION_NOT_FOUND
                        ));
    }

    /**
     * Dùng cho:
     * - AIMessageService.getMessage()
     */
    public AIMessage getMessageOrThrow(
            Long conversationId,
            Long messageId
    ) {

        validateMessageId(messageId);

        return aiMessageRepository
                .findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() ->
                        new ConversationException(
                                ErrorCode.MESSAGE_NOT_FOUND
                        )
                );
    }
}