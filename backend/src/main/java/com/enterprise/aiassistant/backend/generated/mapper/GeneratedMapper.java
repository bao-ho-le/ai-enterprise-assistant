package com.enterprise.aiassistant.backend.generated.mapper;

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
                .aiConversationId(generatedContent.getAiConversationId())
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
                .aiConversationId(generatedContent.getAiConversationId())
                .generatedType(generatedContent.getGeneratedType())
                .title(generatedContent.getTitle())
                .content(generatedContent.getContent())
                .createdAt(generatedContent.getCreatedAt())
                .updatedAt(generatedContent.getUpdatedAt())
                .build();
    }

    public GeneratedContent toCreateGeneratedContentObject(
            Long aiConversationId,
            GeneratedDocumentType generatedType,
            String title,
            String content
    ) {

        return GeneratedContent.builder()
                .aiConversationId(aiConversationId)
                .generatedType(generatedType)
                .title(title.trim())
                .content(content.trim())
                .build();
    }
}
