package com.enterprise.aiassistant.backend.ai.conversation.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    public AIConversationDocument toConversationDocument(DocumentVersion documentVersion) {

        return AIConversationDocument.builder()
                .documentVersion(documentVersion)
                .build();
    }

    public ConversationDetailResponse toDetailResponse(
            AIConversation conversation,
            List<AIConversationDocument> conversationDocuments
    ) {

        List<ConversationDetailResponse.DocumentInfo> documents = conversationDocuments.stream()
                .map(this::toDocumentInfo)
                .toList();

        return ConversationDetailResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getConversationType())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .documents(documents)
                .build();
    }

    private ConversationDetailResponse.DocumentInfo toDocumentInfo(AIConversationDocument conversationDocument) {

        DocumentVersion version = conversationDocument.getDocumentVersion();
        Document document = version.getDocument();

        return new ConversationDetailResponse.DocumentInfo(
                version.getId(),
                document.getId(),
                document.getTitle(),
                version.getVersionNumber()
        );
    }
}
