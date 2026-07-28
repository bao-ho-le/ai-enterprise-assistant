package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AttachDocumentsResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.DocumentQaConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;

import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;

import org.springframework.data.domain.Slice;

import java.util.List;


public interface AIConversationService {

    ConversationResponse createConversation(CreateConversationRequest request);


    ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request);


    void softDeleteConversation(Long conversationId);

    void hardDeleteConversation(Long conversationId);

    AttachDocumentsResponse attachDocuments(Long conversationId, AttachDocumentsRequest request);


    Page<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );

    DocumentQaConversationDetailResponse getDocumentQaConversationDetail(Long conversationId, int recentMessagesLimit);

    GenerationConversationDetailResponse getGenerationConversationDetail(Long conversationId);

    List<ConversationDocumentResponse> getConversationDocuments(Long conversationId);

    void removeDocument(Long conversationId, Long documentVersionId);

    Slice<GeneratedContentResponse> getConversationGeneratedContents(Long conversationId, Pageable pageable);

}
