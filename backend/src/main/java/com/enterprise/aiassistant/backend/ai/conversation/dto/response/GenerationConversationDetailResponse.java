package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.generated.enums.GenerationStatus;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationConversationDetailResponse {

    private Long conversationId;

    private String title;

    private ConversationType conversationType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private GenerationStatus status;

    // Raw JSON text, not JsonNode: the app has both Jackson 2 (langchain4j) and Jackson 3
    // (Spring's default) on the classpath, and the entity's JsonNode is Jackson 2 — Spring's
    // Jackson 3 response writer doesn't recognize it as a tree and would serialize it as a bean.
    @JsonRawValue
    private String inputData;

    // Null khi conversationType = EMAIL_GENERATION
    private List<ConversationDocumentResponse> attachedDocuments;

    private Long generatedContentId;
}
