package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface AIConversationService {

    ConversationResponse createConversation(CreateConversationRequest request);

    ConversationResponse updateConversation(Long conversationId, UpdateConversationRequest request);

    void softDeleteConversation(Long conversationId);

    void hardDeleteConversation(Long conversationId);

    ConversationDetailResponse attachDocuments(Long conversationId, AttachDocumentsRequest request);

    List<ConversationDocumentResponse> getConversationDocuments(Long conversationId);

    void removeDocument(Long conversationId, Long documentVersionId);

    Slice<GeneratedContentResponse> getConversationGeneratedContents(Long conversationId, Pageable pageable);
}
