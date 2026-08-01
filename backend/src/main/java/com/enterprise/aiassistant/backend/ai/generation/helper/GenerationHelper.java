package com.enterprise.aiassistant.backend.ai.generation.helper;

import com.enterprise.aiassistant.backend.ai.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenerationHelper {

    // Spring Boot 4 here auto-configures a Jackson 3 (tools.jackson) ObjectMapper bean,
    // not this Jackson 2 one — JsonNode/ObjectMapper (com.fasterxml.jackson) are only on
    // the classpath transitively via langchain4j, with no bean to @Autowire. Own one directly.
    // FAIL_ON_UNKNOWN_PROPERTIES defaults to true on a bare `new ObjectMapper()` (unlike
    // Spring's usual lenient config) — without disabling it, an inputData field the
    // XxxGenerationInput DTO doesn't recognize yet would hard-fail the whole request.
    private final ObjectMapper objectMapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public void validateTriggerRequest(TriggerGenerationRequest request) {
        if (request == null || request.getInputData() == null) {
            throw new AIConversationException(ErrorCode.GENERATION_INPUT_DATA_REQUIRED);
        }
    }

    public void validateGenerationId(Long generationId) {
        if (generationId == null) {
            throw new AIConversationException(ErrorCode.GENERATION_ID_REQUIRED);
        }

        if (generationId <= 0) {
            throw new AIConversationException(ErrorCode.GENERATION_ID_INVALID);
        }
    }

    // Map (whatever Spring's Jackson 3 converter deserialized the body into) -> the
    // Jackson 2 JsonNode that Generation.inputData and every handler expect.
    public JsonNode toJsonNode(Map<String, Object> inputData) {
        return objectMapper.valueToTree(inputData);
    }

    // Shared by every handler: JsonNode -> typed input DTO, wrapped so a malformed
    // body surfaces as a normal ErrorCode instead of a raw Jackson exception.
    public <T> T parseInput(JsonNode inputData, Class<T> type) {
        try {
            return objectMapper.treeToValue(inputData, type);
        } catch (JsonProcessingException ex) {
            throw new AIConversationException(ErrorCode.GENERATION_INPUT_DATA_INVALID);
        }
    }

    // Report/Summary are grounded in attached documents — DocumentContextService returns
    // an empty string when the conversation has none, which must not reach the model.
    public void validateSourceDocumentsRequired(String documentContext) {
        if (documentContext == null || documentContext.isBlank()) {
            throw new AIConversationException(ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED);
        }
    }

    public String truncateTitle(String raw, int maxLength) {
        String trimmed = raw.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
