package com.enterprise.aiassistant.backend.ai.generation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.TriggerGenerationResponse;

public interface GenerationService {

    TriggerGenerationResponse generate(Long conversationId, TriggerGenerationRequest request);

    GenerationConversationDetailResponse getGenerationDetail(Long generationId);
}
