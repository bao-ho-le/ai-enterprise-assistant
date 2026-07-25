package com.enterprise.aiassistant.backend.ai.conversation.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageSourceResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIMessageMapper {

    /**
     * AIMessage -> AIMessageResponse
     */
    public AIMessageResponse toResponse(AIMessage message) {

        return AIMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * AIMessageSource -> MessageSourceResponse
     */
    public MessageSourceResponse toSourceResponse(
            AIMessageSource source
    ) {

        return MessageSourceResponse.builder()
                .chunkId(source.getChunkId())
                .score(source.getScore())
                .build();
    }

    /**
     * List<AIMessageSource> -> List<MessageSourceResponse>
     */
    public List<MessageSourceResponse> toSourceResponses(
            List<AIMessageSource> sources
    ) {

        return sources.stream()
                .map(this::toSourceResponse)
                .toList();
    }

    /**
     * Build response cho POST /messages
     */
    public MessageResponse toMessageResponse(
            AIMessage userMessage,
            AIMessage assistantMessage
    ) {

        return MessageResponse.builder()
                .userMessage(toResponse(userMessage))
                .assistantMessage(
                        assistantMessage == null
                                ? null
                                : toResponse(assistantMessage)
                )
                .build();
    }

    /**
     * Build message cho sendMessage
     */
    public AIMessage toMessage(
            AIConversation conversation,
            AIMessageRole role,
            String content
    ) {

        return AIMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content.trim())
                .build();
    }

    public MessageDetailResponse toDetailResponse(
            AIMessage message,
            List<AIMessageSource> sources
    ) {

        return MessageDetailResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .sources(toSourceResponses(sources))
                .build();
    }
}