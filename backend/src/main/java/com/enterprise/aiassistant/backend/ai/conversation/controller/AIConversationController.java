package com.enterprise.aiassistant.backend.ai.conversation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("${api.prefix}/ai-conversations")
@RequiredArgsConstructor
public class AIConversationController {

    private final AIConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.createConversation(request));
    }

    @PutMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> renameConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody RenameConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.renameConversation(conversationId, request));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> softDeleteConversation(@PathVariable Long conversationId) {
        conversationService.softDeleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}/hard")
    public ResponseEntity<Void> hardDeleteConversation(@PathVariable Long conversationId) {
        conversationService.hardDeleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/documents")
    public ResponseEntity<ConversationDetailResponse> attachDocuments(
            @PathVariable Long conversationId,
            @Valid @RequestBody AttachDocumentsRequest request
    ) {
        return ResponseEntity.ok(conversationService.attachDocuments(conversationId, request));
    }

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
