package com.enterprise.aiassistant.backend.ai.conversation.controller;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AttachDocumentsResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.DocumentQaConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.service.AIConversationService;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<AttachDocumentsResponse> attachDocuments(
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
    public DocumentQaConversationDetailResponse getDocumentQaConversationDetail(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "20") int recentMessagesLimit
    ) {
        return conversationService.getDocumentQaConversationDetail(conversationId, recentMessagesLimit);
    }

    @GetMapping("/{conversationId}/generation-detail")
    public GenerationConversationDetailResponse getGenerationConversationDetail(
            @PathVariable Long conversationId
    ) {
        return conversationService.getGenerationConversationDetail(conversationId);
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
