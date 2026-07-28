package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIMessageServiceImpl implements AIMessageService {

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final AIMessageMapper messageMapper;
    private final AIMessageHelper messageHelper;

    @Override
    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    ) {

        messageHelper.validateRequest(request);

        AIConversation conversation = getConversationOrThrow(conversationId);

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

        getConversationOrThrow(conversationId);

        Slice<AIMessage> messages =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(
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

        getConversationOrThrow(conversationId);

        AIMessage message = getMessageOrThrow(conversationId, messageId);

        List<AIMessageSource> sources =
                messageSourceRepository.findByAiMessageIdOrderByIdAsc(messageId);

        Map<Long, DocumentChunk> chunksById = loadChunksById(sources);

        return messageMapper.toDetailResponse(message, sources, chunksById);
    }

    private Map<Long, DocumentChunk> loadChunksById(List<AIMessageSource> sources) {

        List<Long> chunkIds = sources.stream().map(AIMessageSource::getChunkId).distinct().toList();

        if (chunkIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return documentChunkRepository.findAllById(chunkIds).stream()
                .collect(Collectors.toMap(DocumentChunk::getId, Function.identity()));
    }

    private AIConversation getConversationOrThrow(Long conversationId) {

        messageHelper.validateConversationId(conversationId);

        return conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private AIMessage getMessageOrThrow(Long conversationId, Long messageId) {

        messageHelper.validateMessageId(messageId);

        return messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.MESSAGE_NOT_FOUND));
    }
}
