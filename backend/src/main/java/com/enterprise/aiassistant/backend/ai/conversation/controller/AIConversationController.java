package com.enterprise.aiassistant.backend.ai.conversation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{id}")
    public ResponseEntity<ConversationResponse> updateConversation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.updateConversation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<ConversationDetailResponse> attachDocuments(
            @PathVariable Long id,
            @Valid @RequestBody AttachDocumentsRequest request
    ) {
        return ResponseEntity.ok(conversationService.attachDocuments(id, request));
    }

    @GetMapping("/{conversationId}/documents")
    public ResponseEntity<List<ConversationDocumentResponse>> getConversationDocuments(
            @PathVariable Long conversationId) {

        return ResponseEntity.ok(
                conversationService.getConversationDocuments(conversationId)
        );
    }


}
