package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {

    private Long id;

    private String title;

    private ConversationType conversationType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
