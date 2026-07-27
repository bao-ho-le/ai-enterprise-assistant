package com.enterprise.aiassistant.backend.ai.conversation.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public AIConversation toEntity(CreateConversationRequest request) {

        return AIConversation.builder()
                .title(request.getTitle())
                .conversationType(request.getConversationType())
                .build();
    }

    public ConversationResponse toResponse(AIConversation conversation) {

        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getConversationType())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    public AIConversationDocument toConversationDocument(
            AIConversation conversation,
            DocumentVersion documentVersion
    ) {

        return AIConversationDocument.builder()
                .conversation(conversation)
                .documentVersion(documentVersion)
                .build();
    }
}
