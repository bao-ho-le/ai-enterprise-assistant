package com.enterprise.aiassistant.backend.ai.conversation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIConversationService;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
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

    @PutMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> updateConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody UpdateConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.updateConversation(conversationId, request));
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

    @GetMapping("/{conversationId}/documents")
    public ResponseEntity<List<ConversationDocumentResponse>> getConversationDocuments(
            @PathVariable Long conversationId) {

        return ResponseEntity.ok(
                conversationService.getConversationDocuments(conversationId)
        );
    }

    @DeleteMapping("/{conversationId}/documents/{documentVersionId}")
    public ResponseEntity<Void> removeDocument(
            @PathVariable Long conversationId,
            @PathVariable Long documentVersionId
    ) {
        conversationService.removeDocument(conversationId, documentVersionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{conversationId}/generated-content")
    public ResponseEntity<Slice<GeneratedContentResponse>> getConversationGeneratedContents(
            @PathVariable Long conversationId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationGeneratedContents(conversationId, pageable)
        );
    }
}
