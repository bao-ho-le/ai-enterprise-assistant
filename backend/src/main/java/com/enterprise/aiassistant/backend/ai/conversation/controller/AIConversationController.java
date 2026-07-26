package com.enterprise.aiassistant.backend.ai.conversation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIConversationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.prefix}/ai-conversations")
@RequiredArgsConstructor
public class AIConversationController {

    private final AIConversationService conversationService;

    @GetMapping
    public Page<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return conversationService.getConversations(filter, pageable);
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse getConversationDetail(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "20") int recentMessagesLimit
    ) {
        return conversationService.getConversationDetail(conversationId, recentMessagesLimit);
    }

    @GetMapping("/{conversationId}/messages")
    public Page<MessageResponse> getConversationMessages(
            @PathVariable Long conversationId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return conversationService.getConversationMessages(conversationId, pageable);
    }
}
