package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;

public interface AIConversationService {

    ConversationResponse createConversation(CreateConversationRequest request);

    ConversationResponse updateConversation(Long id, UpdateConversationRequest request);

    void deleteConversation(Long id);

    ConversationDetailResponse attachDocuments(Long id, AttachDocumentsRequest request);
}
