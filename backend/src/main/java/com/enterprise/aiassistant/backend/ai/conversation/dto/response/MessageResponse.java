package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MessageResponse {
    private AIMessageResponse userMessage;

    private AIMessageResponse assistantMessage;



}
