package com.enterprise.aiassistant.backend.ai.message.controller;

import com.enterprise.aiassistant.backend.ai.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.message.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.message.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.message.service.AIMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/ai-conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class AIMessageController {

    private final AIMessageService messageService;


    @PostMapping
    public MessageResponse sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.sendMessage(conversationId, request);
    }


    @GetMapping
    public Slice<AIMessageResponse> getMessages(
            @PathVariable Long conversationId,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return messageService.getMessages(
                conversationId,
                pageable
        );
    }

    @GetMapping("/{messageId}")
    public MessageDetailResponse getMessageEvidence(
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ) {
        return messageService.getMessageEvidence(conversationId, messageId);
    }
}
