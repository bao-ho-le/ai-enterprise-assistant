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
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.vectorstore.service.VectorStoreService;
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

    private static final int CHAT_TOP_K = 5;

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final PromptBuilderService promptBuilderService;
    private final LLMService llmService;
    private final AIUsageLogService aiUsageLogService;

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
        messageHelper.validateChatConversationType(conversation.getConversationType());

        AIMessage userMessage = messageMapper.toMessage(conversation, AIMessageRole.USER, request.getContent());
        AIMessage savedUserMessage = messageRepository.save(userMessage);

        AIMessage assistantMessage = generateAssistantReply(conversation, request.getContent());

        return messageMapper.toMessageResponse(savedUserMessage, assistantMessage);
    }

    // Embeds the question (real EmbeddingService), retrieves top-K chunks from the
    // conversation's attached document versions (real Qdrant search, post-filtered —
    // VectorStoreService only filters by a single documentId), builds the prompt and
    // calls LLMService (currently FakeLLMService). Never throws: a retrieval/LLM
    // failure just means no assistant reply, the user's message still stands.
    private AIMessage generateAssistantReply(AIConversation conversation, String question) {

        List<Long> attachedVersionIds =
                conversationDocumentRepository.findDocumentVersionIdsByConversationId(conversation.getId());

        String model = llmService.getModelName();
        Integer inputTokens = null;
        Integer outputTokens = null;

        try {
            List<SearchResult> relevantHits = attachedVersionIds.isEmpty()
                    ? List.of()
                    : retrieveRelevantChunks(question, attachedVersionIds);

            String prompt = promptBuilderService.buildDocumentQaPrompt(
                    question,
                    relevantHits.stream().map(hit -> hit.getPayload().getContent()).toList()
            );

            LLMResponse llmResponse = llmService.generate(
                    LLMRequest.builder()
                            .prompt(prompt)
                            .conversationType(conversation.getConversationType())
                            .build()
            );
            model = llmResponse.getModelName();
            inputTokens = llmResponse.getTokenUsage().getInputTokens();
            outputTokens = llmResponse.getTokenUsage().getOutputTokens();

            AIMessage assistantMessage = messageRepository.save(
                    messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, llmResponse.getContent())
            );

            if (!relevantHits.isEmpty()) {
                messageSourceRepository.saveAll(
                        relevantHits.stream().map(hit -> toMessageSource(assistantMessage, hit)).toList()
                );
            }

            logChatUsage(conversation, assistantMessage.getId(), model, inputTokens, outputTokens,
                    AIUsageStatus.SUCCESS, null);

            return assistantMessage;

        } catch (RuntimeException ex) {
            logChatUsage(conversation, null, model, inputTokens, outputTokens, AIUsageStatus.FAILED, ex.getMessage());
            return null;
        }
    }

    // VectorStoreService.search() only takes a single documentId filter, so we search
    // globally and keep only hits whose documentVersionId is attached to this
    // conversation — the same version the conversation was pinned to at attach time.
    private List<SearchResult> retrieveRelevantChunks(String question, List<Long> attachedVersionIds) {

        EmbeddingResult queryEmbedding = embeddingService.embed(question);

        return vectorStoreService.search(queryEmbedding.getVector(), CHAT_TOP_K, null).stream()
                .filter(hit -> attachedVersionIds.contains(hit.getPayload().getDocumentVersionId()))
                .toList();
    }

    private AIMessageSource toMessageSource(AIMessage assistantMessage, SearchResult hit) {

        VectorPayload payload = hit.getPayload();

        return AIMessageSource.builder()
                .aiMessage(assistantMessage)
                .documentChunk(documentChunkRepository.getReferenceById(payload.getChunkId()))
                .similarityScore(hit.getScore())
                .documentVersionId(payload.getDocumentVersionId())
                .chunkId(payload.getChunkId())
                .score(hit.getScore())
                .build();
    }

    private void logChatUsage(
            AIConversation conversation,
            Long messageId,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            AIUsageStatus status,
            String errorMessage
    ) {
        aiUsageLogService.logAiUsage(AIUsageLogRequest.builder()
                .conversationId(conversation.getId())
                .messageId(messageId)
                .conversationType(conversation.getConversationType())
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .status(status)
                .errorMessage(errorMessage)
                .build());
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
