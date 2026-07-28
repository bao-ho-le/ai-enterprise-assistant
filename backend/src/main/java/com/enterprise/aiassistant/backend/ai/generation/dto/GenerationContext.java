package com.enterprise.aiassistant.backend.ai.generation.dto;

import com.enterprise.aiassistant.backend.generated.enums.GeneratedType;
import lombok.Builder;
import lombok.Getter;

// What a GenerationHandler hands back to GenerationService: the prompt to send to the
// LLM, plus enough to persist the GenerationRun/GeneratedContent rows.
@Getter
@Builder
public class GenerationContext {

    private final String prompt;

    private final String title;

    private final GeneratedType generatedType;
}
