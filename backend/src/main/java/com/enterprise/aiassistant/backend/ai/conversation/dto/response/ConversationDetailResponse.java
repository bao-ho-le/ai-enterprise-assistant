package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ConversationDetailResponse(
        Long id,
        String title,
        ConversationType conversationType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DocumentInfo> documents
) {

    public record DocumentInfo(
            Long documentVersionId,
            Long documentId,
            String title,
            Integer versionNumber
    ) {
    }
}
