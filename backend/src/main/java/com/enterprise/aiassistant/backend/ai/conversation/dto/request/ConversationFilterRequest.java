package com.enterprise.aiassistant.backend.ai.conversation.dto.request;

import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationType;
import lombok.Data;

@Data
public class ConversationFilterRequest {

    private ConversationType conversationType;

    // Defaults to ACTIVE when not provided.
    private ConversationStatus status;
}
