package com.enterprise.aiassistant.backend.ai.conversation.dto.request;

import lombok.Data;

// Dùng cho API gửi message USER (chưa implement trong lượt này)
@Data
public class SendMessageRequest {

    private String content;
}
