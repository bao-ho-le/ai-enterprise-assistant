package com.enterprise.aiassistant.backend.ai.generation.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.generation.enums.GenerationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenerationMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode toJsonNode(Map<String, Object> inputData) {
        return objectMapper.valueToTree(inputData);
    }

    public Generation toGeneration(
            AIConversation conversation,
            GenerationContext context,
            JsonNode inputData
    ) {
        return Generation.builder()
                .aiConversation(conversation)
                .generatedType(context.getGeneratedType())
                .title(context.getTitle())
                .userPrompt(context.getPrompt())
                .inputData(inputData)
                .status(GenerationStatus.PENDING)
                .build();
    }

    public TriggerGenerationResponse toTriggerGenerationResponse(
            Generation generation,
            GeneratedContent generatedContent
    ) {
        return TriggerGenerationResponse.builder()
                .generationId(generation.getId())
                .status(generation.getStatus())
                .generatedContentId(generatedContent.getId())
                .build();
    }
}
