package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.helper.EmbeddingHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.chat.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@RequiredArgsConstructor
public class DocumentQAServiceImpl implements DocumentQAService {

    private static final int CHAT_TOP_K = 5;

    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingHelper embeddingHelper;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final VectorStoreHelper vectorStoreHelper;
    private final VectorStoreMapper vectorStoreMapper;
    private final ConversationMemoryService conversationMemoryService;
    private final PromptBuilderService promptBuilderService;
    private final ChatAssistant chatAssistant;
    private final LLMHelper llmHelper;
    private final AIUsageLogService aiUsageLogService;

    private final AIMessageMapper messageMapper;
    private final AIConversationHelper aiConversationHelper;
    private final DocumentAuthorizationService documentAuthorizationService;

    @Override
    public AIMessage answer(AIConversation conversation, String question) {

        List<AIConversationDocument> attachedDocuments =
                conversationDocumentRepository.findByAiConversationIdWithDocument(conversation.getId());

        // Không build context / không gọi LLM nếu có tài liệu đính kèm đã bị soft-delete
        aiConversationHelper.validateAttachedDocumentsNotDeleted(attachedDocuments);

        // Quyền có thể bị thu hồi sau khi attach — lọc lại ngay trước khi build context cho LLM.
        Set<Long> readableDocumentIds = documentAuthorizationService.filterReadableDocumentIds(
                attachedDocuments.stream()
                        .map(link -> link.getDocumentVersion().getDocument())
                        .filter(java.util.Objects::nonNull)
                        .toList()
        );

        List<Long> attachedVersionIds = attachedDocuments.stream()
                .filter(link -> isReadable(link.getDocumentVersion().getDocument(), readableDocumentIds))
                .map(link -> link.getDocumentVersion().getId())
                .toList();

        String model = llmHelper.getModelName();
        Integer inputTokens = null;
        Integer outputTokens = null;
        Long messageId = null;

        try {
            // Context các lượt trước (chưa gồm câu hỏi hiện tại): giúp LLM hiểu tham chiếu
            String conversationMemory = conversationMemoryService.buildMemoryContext(conversation.getId());

            List<EmbeddingMatch<TextSegment>> relevantHits = attachedVersionIds.isEmpty()
                    ? List.of()
                    : retrieveRelevantChunks(question, attachedVersionIds);

            String documentExcerpts = promptBuilderService.buildDocumentExcerpts(
                    relevantHits.stream().map(hit -> hit.embedded().text()).toList()
            );

            Result<String> answer = llmHelper.call(() ->
                    chatAssistant.answerFromDocuments(question, conversationMemory, documentExcerpts)
            );

            if (answer.tokenUsage() != null) {
                inputTokens = answer.tokenUsage().inputTokenCount();
                outputTokens = answer.tokenUsage().outputTokenCount();
            }

            AIMessage assistantMessage = messageRepository.save(
                    messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, answer.content())
            );

            if (!relevantHits.isEmpty()) {
                messageSourceRepository.saveAll(
                        relevantHits.stream().map(hit -> toMessageSource(assistantMessage, hit)).toList()
                );
            }

            messageId = assistantMessage.getId();
            aiUsageLogService.logAiUsage(
                    AIUsageLogRequest.builder()
                            .conversationId(conversation.getId())
                            .messageId(messageId)
                            .conversationType(conversation.getConversationType())
                            .model(model)
                            .inputTokens(inputTokens)
                            .outputTokens(outputTokens)
                            .status(AIUsageStatus.SUCCESS)
                            .build()
            );

            return assistantMessage;

        } catch (RuntimeException ex) {
            aiUsageLogService.logAiUsage(
                    AIUsageLogRequest.builder()
                            .conversationId(conversation.getId())
                            .messageId(messageId)
                            .conversationType(conversation.getConversationType())
                            .model(model)
                            .inputTokens(inputTokens)
                            .outputTokens(outputTokens)
                            .status(AIUsageStatus.FAILED)
                            .build()
            );
            return null;
        }
    }


    // Helper

    private boolean isReadable(Document document, Set<Long> readableDocumentIds) {
        return document != null && readableDocumentIds.contains(document.getId());
    }

    // Chỉ lấy tối đa 5 chunks có độ liên quan cao nhất (CHAT_TOP_K) trong các tài liệu đính kèm.
    // Lọc theo version ngay trong Qdrant, không lấy top-K toàn kho rồi mới lọc (cách cũ có thể mất
    // hết kết quả khi tài liệu khác điểm cao hơn); ngưỡng điểm vẫn như semantic search.
    private List<EmbeddingMatch<TextSegment>> retrieveRelevantChunks(String question, List<Long> attachedVersionIds) {

        Embedding queryEmbedding = embeddingHelper.call(() -> embeddingModel.embed(question)).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(CHAT_TOP_K)
                .minScore(vectorStoreHelper.minScore())
                .filter(metadataKey(VectorStoreMapper.DOCUMENT_VERSION_ID).isIn(attachedVersionIds))
                .build();

        return vectorStoreHelper.call(ErrorCode.VECTOR_SEARCH_FAILED, () -> embeddingStore.search(request).matches());
    }

    private AIMessageSource toMessageSource(AIMessage assistantMessage, EmbeddingMatch<TextSegment> hit) {

        Long chunkId = vectorStoreMapper.chunkId(hit);
        double score = vectorStoreMapper.cosineScore(hit);

        return AIMessageSource.builder()
                .aiMessage(assistantMessage)
                .documentChunk(documentChunkRepository.getReferenceById(chunkId))
                .similarityScore(score)
                .documentVersionId(vectorStoreMapper.documentVersionId(hit))
                .chunkId(chunkId)
                .score(score)
                .build();
    }

}
