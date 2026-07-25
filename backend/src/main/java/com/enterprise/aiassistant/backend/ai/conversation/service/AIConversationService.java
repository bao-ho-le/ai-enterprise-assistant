package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AIConversationService {

    Page<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );

    ConversationDetailResponse getConversationDetail(Long conversationId, int recentMessagesLimit);

    Page<MessageResponse> getConversationMessages(Long conversationId, Pageable pageable);
}
