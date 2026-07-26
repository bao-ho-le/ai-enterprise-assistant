package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;

public interface AIConversationService {

    ConversationResponse createConversation(CreateConversationRequest request);

    ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request);

    void softDeleteConversation(Long conversationId);

    void hardDeleteConversation(Long conversationId);

    ConversationDetailResponse attachDocuments(Long conversationId, AttachDocumentsRequest request);
}
