package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;

    private String title;

    private ConversationType conversationType;

    private ConversationStatus status;

    private Long messageCount;

    private OffsetDateTime lastMessageAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
