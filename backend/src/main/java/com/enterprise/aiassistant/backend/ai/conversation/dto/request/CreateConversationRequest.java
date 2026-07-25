package com.enterprise.aiassistant.backend.ai.conversation.dto.request;

import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Conversation type is required")
    private ConversationType conversationType;
}
