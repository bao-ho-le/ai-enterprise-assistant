package com.enterprise.aiassistant.backend.ai.message.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.message.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.qa.service.DocumentQAService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    private final DocumentQAService documentQAService;


    @Override
    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    ) {

        messageHelper.validateRequest(request);

        AIConversation conversation = getConversationOrThrow(conversationId);
        messageHelper.validateChatConversationType(conversation.getConversationType());

        AIMessage userMessage = messageMapper.toMessage(conversation, AIMessageRole.USER, request.getContent());
        AIMessage savedUserMessage = messageRepository.save(userMessage);

        AIMessage assistantMessage = documentQAService.answer(conversation, request.getContent());

        return messageMapper.toMessageResponse(savedUserMessage, assistantMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(
            Long conversationId,
            Long beforeId,
            int size
    ) {

        getConversationOrThrow(conversationId);

        Slice<AIMessage> page = messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                conversationId,
                beforeId != null ? beforeId : Long.MAX_VALUE,
                PageRequest.of(0, size)
        );

        return messageMapper.toMessagePageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageDetailResponse getMessageEvidence(
            Long conversationId,
            Long messageId
    ) {

        getConversationOrThrow(conversationId);

        AIMessage message = getMessageOrThrow(conversationId, messageId);

        List<AIMessageSource> sources =
                messageSourceRepository.findByAiMessageIdOrderByIdAsc(messageId);

        Map<Long, DocumentChunk> chunksById = loadEvidenceChunks(sources);

        return messageMapper.toDetailResponse(message, sources, chunksById);
    }


    // Helper

    private Map<Long, DocumentChunk> loadEvidenceChunks(List<AIMessageSource> sources) {

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
