package com.enterprise.aiassistant.backend.ai.generation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.generation.service.GenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationService generationService;

    @PostMapping("/ai-conversations/{conversationId}/generate")
    public TriggerGenerationResponse generate(
            @PathVariable Long conversationId,
            @Valid @RequestBody TriggerGenerationRequest request
    ) {
        return generationService.generate(conversationId, request);
    }

    @GetMapping("/ai-conversations/generations/{generationId}")
    public GenerationConversationDetailResponse getGenerationDetail(
            @PathVariable Long generationId
    ) {
        return generationService.getGenerationDetail(generationId);
    }
}
