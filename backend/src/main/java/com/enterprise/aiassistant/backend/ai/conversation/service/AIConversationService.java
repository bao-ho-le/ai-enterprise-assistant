package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AttachDocumentsResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.DocumentQaConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import org.springframework.data.domain.Pageable;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.StartDocumentQaConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.StartGenerationConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.StartDocumentQaConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.StartGenerationConversationResponse;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;

import com.enterprise.aiassistant.backend.ai.generation.dto.response.GenerationResponse;

import org.springframework.data.domain.Slice;

import java.util.List;


public interface AIConversationService {

    ConversationResponse createConversation(CreateConversationRequest request);


    ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request);


    void softDeleteConversation(Long conversationId);

    void hardDeleteConversation(Long conversationId);

    AttachDocumentsResponse attachDocuments(Long conversationId, AttachDocumentsRequest request);

    // Atomic: create + attach + first message in one transaction, so a failure at any
    // step rolls back the whole thing — no orphaned empty conversation can result.
    StartDocumentQaConversationResponse startDocumentQaConversation(StartDocumentQaConversationRequest request);

    // Atomic: create + attach (if any) + generate in one transaction, so a failure at any
    // step rolls back the whole thing — no orphaned empty conversation can result.
    StartGenerationConversationResponse startGenerationConversation(StartGenerationConversationRequest request);


    Slice<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );

    // Soft-deleted conversations only — status is forced server-side, the filter's own
    // status field is ignored here.
    Slice<ConversationResponse> getDeletedConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );

    DocumentQaConversationDetailResponse getDocumentQaConversationDetail(Long conversationId, int recentMessagesLimit);

    GenerationConversationDetailResponse getGenerationConversationDetail(Long conversationId);

    List<ConversationDocumentResponse> getConversationDocuments(Long conversationId);

    void removeDocument(Long conversationId, Long documentVersionId);

    Slice<GenerationResponse> getConversationGenerations(Long conversationId, Pageable pageable);

}
