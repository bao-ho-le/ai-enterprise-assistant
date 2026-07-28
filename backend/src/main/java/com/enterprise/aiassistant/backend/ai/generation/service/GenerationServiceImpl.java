package com.enterprise.aiassistant.backend.ai.generation.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.generation.handler.GenerationHandler;
import com.enterprise.aiassistant.backend.ai.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.entity.GenerationRun;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.generated.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.generated.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.generated.repository.GenerationRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {

    private final AIConversationRepository conversationRepository;
    private final GenerationRunRepository generationRunRepository;
    private final GeneratedContentRepository generatedContentRepository;

    private final AIConversationHelper aiConversationHelper;
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

        GenerationRun run = generationRunRepository.save(
                GenerationRun.builder()
                        .aiConversation(conversation)
                        .generatedType(context.getGeneratedType())
                        .title(context.getTitle())
                        .userPrompt(context.getPrompt())
                        .inputData(inputData)
                        .status(GenerationStatus.PENDING)
                        .build()
        );

        run.setStatus(GenerationStatus.RUNNING);
        generationRunRepository.save(run);

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
            inputTokens = llmResponse.getTokenUsage().getInputTokens();
            outputTokens = llmResponse.getTokenUsage().getOutputTokens();

            GeneratedContent generatedContent = generatedContentRepository.save(
                    generatedMapper.toCreateGeneratedContentObject(
                            conversation,
                            GeneratedDocumentType.valueOf(context.getGeneratedType().name()),
                            context.getTitle(),
                            llmResponse.getContent()
                    )
            );

            run.setGeneratedContent(generatedContent);
            run.setStatus(GenerationStatus.COMPLETED);
            generationRunRepository.save(run);

            logUsage(conversation, model, inputTokens, outputTokens, AIUsageStatus.SUCCESS, null);

            return TriggerGenerationResponse.builder()
                    .generationRunId(run.getId())
                    .status(run.getStatus())
                    .generatedContentId(generatedContent.getId())
                    .build();

        } catch (RuntimeException ex) {
            run.setStatus(GenerationStatus.FAILED);
            run.setErrorMessage(ex.getMessage());
            generationRunRepository.save(run);

            logUsage(conversation, model, inputTokens, outputTokens, AIUsageStatus.FAILED, ex.getMessage());

            throw ex;
        }
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
