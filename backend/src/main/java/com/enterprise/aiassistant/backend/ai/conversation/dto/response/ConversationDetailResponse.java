package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationType;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {

    private Long id;

    private String title;

    private ConversationType conversationType;

    private ConversationStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private List<AttachedDocumentItem> documents;

    // Chỉ chứa N message gần nhất (mặc định 20, chỉnh bằng query param `recentMessages`).
    // Lấy đầy đủ + phân trang: gọi GET /ai-conversations/{id}/messages
    private List<MessageResponse> recentMessages;

    private Long totalMessages;

    private List<GeneratedDocumentItem> generatedDocuments;

    // Tổng hợp từ ai_usage_logs cho riêng conversation này
    private Long totalTokens;

    private BigDecimal estimatedCost;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachedDocumentItem {
        private Long documentVersionId;
        private Long documentId;
        private String documentTitle;
        private Integer versionNumber;
        private OffsetDateTime attachedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedDocumentItem {
        private Long id;
        private GeneratedDocumentType generatedType;
        private String title;
        private OffsetDateTime createdAt;
    }
}
