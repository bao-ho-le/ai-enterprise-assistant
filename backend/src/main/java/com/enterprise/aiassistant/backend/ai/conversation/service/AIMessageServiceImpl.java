package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.conversation.helper.ConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIMessageServiceImpl implements AIMessageService {

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository aiMessageSourceRepository;

    private final AIMessageMapper messageMapper;
    private final AIMessageHelper messageHelper;
    private final ConversationHelper conversationHelper;

    @Override
    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    ) {

        messageHelper.validateRequest(request);

        AIConversation conversation = conversationHelper.getConversationOrThrow(conversationId);

        AIMessage userMessage = messageMapper.toMessage(conversation, AIMessageRole.USER, request.getContent());

        AIMessage savedMessage = messageRepository.save(userMessage);

        return messageMapper.toMessageResponse(savedMessage, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AIMessageResponse> getMessages(
            Long conversationId,
            Pageable pageable
    ) {

        conversationHelper.getConversationOrThrow(conversationId);

        Slice<AIMessage> messages =
                aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(
                        conversationId,
                        pageable
                );

        return messages.map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageDetailResponse getMessageDetail(
            Long conversationId,
            Long messageId
    ) {

        conversationHelper.getConversationOrThrow(conversationId);

        AIMessage message =
                messageHelper.getMessageOrThrow(
                        conversationId,
                        messageId
                );

        List<AIMessageSource> sources =
                aiMessageSourceRepository
                        .findByMessageIdOrderByIdAsc(
                                messageId
                        );

        return messageMapper.toDetailResponse(
                message,
                sources
        );
    }
}
