package com.enterprise.aiassistant.backend.ai.conversation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIMessageService;
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

        return messageService.sendMessage(
                             conversationId,
                             request
                );
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
}
