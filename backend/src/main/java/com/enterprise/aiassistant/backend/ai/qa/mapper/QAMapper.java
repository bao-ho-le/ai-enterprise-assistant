package com.enterprise.aiassistant.backend.ai.qa.mapper;


import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import org.springframework.stereotype.Component;

@Component
public class QAMapper {

    public LLMRequest toLLMRequest(
            String prompt,
            ConversationType conversationType,
            int contextItemCount
    ) {
        return LLMRequest.builder()
                .prompt(prompt)
                .conversationType(conversationType)
                .contextItemCount(contextItemCount)
                .build();
    }
}
