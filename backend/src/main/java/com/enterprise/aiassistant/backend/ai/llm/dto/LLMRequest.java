package com.enterprise.aiassistant.backend.ai.llm.dto;

import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LLMRequest {

    private String prompt;

    // Drives the shape of the fake response (email vs report vs QA answer, etc).
    private ConversationType conversationType;
}
