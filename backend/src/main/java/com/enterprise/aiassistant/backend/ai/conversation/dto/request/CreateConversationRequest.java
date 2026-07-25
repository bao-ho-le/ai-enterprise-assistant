package com.enterprise.aiassistant.backend.ai.conversation.dto.request;

import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationType;
import lombok.Data;

// Dùng cho API tạo conversation (chưa implement trong lượt này)
@Data
public class CreateConversationRequest {

    private String title;

    private ConversationType conversationType;
}
