package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;


public interface AIMessageService {

    MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    );

    Slice<AIMessageResponse> getMessages(
            Long conversationId,
            Pageable pageable
    );

    MessageDetailResponse getMessageDetail(
            Long conversationId,
            Long messageId
    );
}
