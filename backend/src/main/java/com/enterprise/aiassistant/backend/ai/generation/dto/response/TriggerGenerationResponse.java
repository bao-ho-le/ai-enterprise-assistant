package com.enterprise.aiassistant.backend.ai.generation.dto.response;

import com.enterprise.aiassistant.backend.generated.enums.GenerationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TriggerGenerationResponse {

    private Long generationRunId;

    private GenerationStatus status;

    private Long generatedContentId;

    private String errorMessage;
}
