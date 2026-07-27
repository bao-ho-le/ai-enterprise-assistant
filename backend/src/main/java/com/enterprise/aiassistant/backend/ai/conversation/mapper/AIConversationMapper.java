package com.enterprise.aiassistant.backend.ai.conversation.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageSourceResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AIConversationMapper {

    public ConversationDetailResponse.AttachedDocumentItem toAttachedDocumentItem(
            AIConversationDocument conversationDocument
    ) {
        DocumentVersion version = conversationDocument.getDocumentVersion();

        return ConversationDetailResponse.AttachedDocumentItem.builder()
                .documentVersionId(version.getId())
                .documentId(version.getDocument().getId())
                .documentTitle(version.getDocument().getTitle())
                .versionNumber(version.getVersionNumber())
                .attachedAt(conversationDocument.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .build();
    }

    public MessageSourceResponse toMessageSourceResponse(AIMessageSource source) {
        DocumentChunk chunk = source.getDocumentChunk();
        DocumentVersion version = chunk.getDocumentVersion();

        return MessageSourceResponse.builder()
                .id(source.getId())
                .documentChunkId(chunk.getId())
                .documentId(version.getDocument().getId())
                .documentTitle(version.getDocument().getTitle())
                .chunkIndex(chunk.getChunkIndex())
                .pageNumber(chunk.getPageNumber())
                .contentSnippet(
                        chunk.getContent().length() > 300
                                ? chunk.getContent().substring(0, 300) + "..."
                                : chunk.getContent()
                )
                .similarityScore(source.getSimilarityScore())
                .build();
    }

    public MessageResponse toMessageResponse(
            AIMessage message,
            Map<Long, List<AIMessageSource>> sourcesByMessageId
    ) {
        List<AIMessageSource> sources =
                sourcesByMessageId.getOrDefault(message.getId(), Collections.emptyList());

        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .sources(sources.stream().map(this::toMessageSourceResponse).toList())
                .build();
    }

    public ConversationDetailResponse.GeneratedDocumentItem toGeneratedDocumentItem(
            GeneratedContent content
    ) {
        return ConversationDetailResponse.GeneratedDocumentItem.builder()
                .id(content.getId())
                .generatedType(content.getGeneratedType())
                .title(content.getTitle())
                // GeneratedContent.createdAt là LocalDateTime (module generated chưa đổi) -> quy đổi sang OffsetDateTime theo timezone hệ thống
                .createdAt(content.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .build();
    }

    public ConversationDetailResponse toDetailResponse(
            AIConversation conversation,
            List<AIConversationDocument> documents,
            List<AIMessage> recentMessages,
            long totalMessages,
            Map<Long, List<AIMessageSource>> sourcesByMessageId,
            List<GeneratedContent> generatedContents,
            Long totalTokens,
            BigDecimal estimatedCost
    ) {
        return ConversationDetailResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getConversationType())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .updatedAt(conversation.getUpdatedAt() != null
                        ? conversation.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                        : null)
                .documents(documents.stream().map(this::toAttachedDocumentItem).toList())
                .recentMessages(
                        recentMessages.stream()
                                .map(m -> toMessageResponse(m, sourcesByMessageId))
                                .toList()
                )
                .totalMessages(totalMessages)
                .generatedDocuments(
                        generatedContents.stream().map(this::toGeneratedDocumentItem).toList()
                )
                .totalTokens(totalTokens != null ? totalTokens : 0L)
                .estimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO)
                .build();
    }
}
