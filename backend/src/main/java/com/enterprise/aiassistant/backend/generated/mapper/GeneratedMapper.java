package com.enterprise.aiassistant.backend.generated.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import org.springframework.stereotype.Component;

@Component
public class GeneratedMapper {

    public GeneratedContentResponse toGeneratedContentResponse(
            GeneratedContent generatedContent
    ) {
        return GeneratedContentResponse.builder()
                .id(generatedContent.getId())
                .aiConversationId(generatedContent.getAiConversation().getId())
                .generatedType(generatedContent.getGeneratedType())
                .title(generatedContent.getTitle())
                .createdAt(generatedContent.getCreatedAt())
                .updatedAt(generatedContent.getUpdatedAt())
                .build();
    }

    public GeneratedContentDetailResponse toGeneratedContentDetailResponse(
            GeneratedContent generatedContent
    ) {
        return GeneratedContentDetailResponse.builder()
                .id(generatedContent.getId())
                .aiConversationId(generatedContent.getAiConversation().getId())
                .generatedType(generatedContent.getGeneratedType())
                .title(generatedContent.getTitle())
                .content(generatedContent.getContent())
                .createdAt(generatedContent.getCreatedAt())
                .updatedAt(generatedContent.getUpdatedAt())
                .build();
    }

    public GeneratedContent toCreateGeneratedContentObject(
            AIConversation aiConversation,
            GeneratedDocumentType generatedType,
            String title,
            String content
    ) {

        return GeneratedContent.builder()
                .aiConversation(aiConversation)
                .generatedType(generatedType)
                .title(title.trim())
                .content(content.trim())
                .build();
    }
}
