package com.enterprise.aiassistant.backend.ai.generation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.ai.generation.handler.GenerationHandler;
import com.enterprise.aiassistant.backend.ai.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {

    private final AIConversationRepository conversationRepository;
    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final GenerationRepository generationRepository;
    private final GeneratedContentRepository generatedContentRepository;

    private final AIConversationHelper aiConversationHelper;
    private final AIConversationMapper aiConversationMapper;
    private final GenerationHelper generationHelper;
    private final GeneratedMapper generatedMapper;
    private final AIUsageLogService aiUsageLogService;
    private final LLMService llmService;

    private final List<GenerationHandler> handlers;

    @Override
    @Transactional
    public TriggerGenerationResponse generate(Long conversationId, TriggerGenerationRequest request) {

        aiConversationHelper.validateConversationId(conversationId);
        generationHelper.validateTriggerRequest(request);

        AIConversation conversation = conversationRepository
                .findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateGenerationConversationType(conversation.getConversationType());

        GenerationHandler handler = handlers.stream()
                .filter(h -> h.supports(conversation.getConversationType()))
                .findFirst()
                .orElseThrow(() -> new AIConversationException(ErrorCode.GENERATION_HANDLER_NOT_FOUND));

        JsonNode inputData = generationHelper.toJsonNode(request.getInputData());

        GenerationContext context = handler.handle(inputData, conversation);

        Generation generation = generationRepository.save(
                Generation.builder()
                        .aiConversation(conversation)
                        .generatedType(context.getGeneratedType())
                        .title(context.getTitle())
                        .userPrompt(context.getPrompt())
                        .inputData(inputData)
                        .status(GenerationStatus.PENDING)
                        .build()
        );

        generation.setStatus(GenerationStatus.RUNNING);
        generationRepository.save(generation);

        String model = llmService.getModelName();
        Integer inputTokens = null;
        Integer outputTokens = null;

        try {
            LLMResponse llmResponse = llmService.generate(
                    LLMRequest.builder()
                            .prompt(context.getPrompt())
                            .conversationType(conversation.getConversationType())
                            .build()
            );
            model = llmResponse.getModelName();
            if (llmResponse.getTokenUsage() != null) {
                inputTokens = llmResponse.getTokenUsage().getInputTokens();
                outputTokens = llmResponse.getTokenUsage().getOutputTokens();
            }

            GeneratedContent generatedContent = generatedContentRepository.save(
                    generatedMapper.toCreateGeneratedContentObject(
                            GeneratedDocumentType.valueOf(context.getGeneratedType().name()),
                            context.getTitle(),
                            llmResponse.getContent()
                    )
            );

            generation.setGeneratedContent(generatedContent);
            generation.setStatus(GenerationStatus.COMPLETED);
            generationRepository.save(generation);

            logUsage(conversation, model, inputTokens, outputTokens, AIUsageStatus.SUCCESS, null);

            return TriggerGenerationResponse.builder()
                    .generationId(generation.getId())
                    .status(generation.getStatus())
                    .generatedContentId(generatedContent.getId())
                    .build();

        } catch (RuntimeException ex) {
            generation.setStatus(GenerationStatus.FAILED);
            generation.setErrorMessage(ex.getMessage());
            generationRepository.save(generation);

            logUsage(conversation, model, inputTokens, outputTokens, AIUsageStatus.FAILED, ex.getMessage());

            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationConversationDetailResponse getGenerationDetail(Long generationId) {

        generationHelper.validateGenerationId(generationId);

        Generation generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.GENERATION_NOT_FOUND));

        AIConversation conversation = generation.getAiConversation();

        // Nếu conversation type là email thì không có attach document
        List<ConversationDocumentResponse> attachedDocuments =
                conversation.getConversationType() == ConversationType.EMAIL_GENERATION
                        ? null
                        : conversationDocumentRepository.findByAiConversationIdWithDocument(conversation.getId())
                                .stream()
                                .map(aiConversationMapper::toConversationDocumentResponse)
                                .toList();

        return aiConversationMapper.toGenerationDetailResponse(conversation, generation, attachedDocuments);
    }

    private void logUsage(
            AIConversation conversation,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            AIUsageStatus status,
            String errorMessage
    ) {
        aiUsageLogService.logAiUsage(AIUsageLogRequest.builder()
                .conversationId(conversation.getId())
                .conversationType(conversation.getConversationType())
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }
}
