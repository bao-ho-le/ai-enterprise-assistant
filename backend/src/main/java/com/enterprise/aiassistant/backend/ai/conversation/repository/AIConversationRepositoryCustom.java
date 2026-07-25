package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AIConversationRepositoryCustom {

    Page<ConversationResponse> filterConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    );
}
