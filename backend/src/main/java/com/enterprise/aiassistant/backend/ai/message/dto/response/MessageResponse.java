package com.enterprise.aiassistant.backend.ai.message.dto.response;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class MessageResponse {
    private AIMessageResponse userMessage;

    private AIMessageResponse assistantMessage;

}
