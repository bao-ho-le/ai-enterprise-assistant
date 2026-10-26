package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.chat.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.config.DocumentQAConfig;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentQAServiceImpl implements DocumentQAService {

    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final VectorStoreMapper vectorStoreMapper;
    private final ConversationMemoryService conversationMemoryService;
    private final DocumentQAAssistant documentQAAssistant;
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

            // Tìm đoạn liên quan trong các version đính kèm rồi nối vào prompt: LangChain4j tự làm
            // trong lúc gọi (RetrievalAugmentor ở DocumentQAConfig), answer.sources() là các đoạn đã dùng.
            InvocationParameters retrievalParameters = InvocationParameters.from(Map.of(
                    DocumentQAConfig.QUESTION, question,
                    DocumentQAConfig.DOCUMENT_VERSION_IDS, attachedVersionIds
            ));

            Result<String> answer = llmHelper.call(() ->
                    documentQAAssistant.answer(question, conversationMemory, retrievalParameters)
            );

            if (answer.tokenUsage() != null) {
                inputTokens = answer.tokenUsage().inputTokenCount();
                outputTokens = answer.tokenUsage().outputTokenCount();
            }

            AIMessage assistantMessage = messageRepository.save(
                    messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, answer.content())
            );

            List<Content> relevantHits = answer.sources();

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
            // Embedding/tìm kiếm giờ chạy bên trong AI Service, không còn qua EmbeddingHelper (nơi ghi log
            // lỗi), nên ghi log ở đây để lỗi không bị nuốt mất.
            log.error("Document QA failed for conversation {}", conversation.getId(), ex);
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

    private AIMessageSource toMessageSource(AIMessage assistantMessage, Content hit) {

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
