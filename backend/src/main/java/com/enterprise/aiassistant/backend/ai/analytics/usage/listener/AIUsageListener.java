package com.enterprise.aiassistant.backend.ai.analytics.usage.listener;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelErrorContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// Ghi AI usage log cho mọi lượt gọi Gemini, gắn vào ChatModel (GeminiConfig) và EmbeddingModel
// (GeminiEmbeddingModelConfig) nên không phụ thuộc lời gọi đi từ AI Service, ChatModel hay luồng RAG.
// Loại thao tác và conversation/generation lấy từ AIUsageContext. LangChain4j bỏ qua lỗi ném ra từ
// listener, nên ghi log lỗi không làm hỏng lượt gọi AI.
@Slf4j
@Component
@RequiredArgsConstructor
public class AIUsageListener implements ChatModelListener, EmbeddingModelListener {

    // Ước lượng token: ~4 ký tự/token (cùng quy ước với ChunkingMapper.CHARS_PER_TOKEN).
    private static final int CHARS_PER_TOKEN = 4;

    private final AIUsageLogService aiUsageLogService;

    @Override
    public void onResponse(ChatModelResponseContext context) {

        TokenUsage tokenUsage = context.chatResponse().tokenUsage();
        String responseModel = context.chatResponse().metadata().modelName();

        logUsage(
                responseModel != null ? responseModel : context.chatRequest().modelName(),
                tokenUsage != null ? tokenUsage.inputTokenCount() : null,
                tokenUsage != null ? tokenUsage.outputTokenCount() : null,
                AIUsageStatus.SUCCESS,
                null
        );
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        logUsage(context.chatRequest().modelName(), null, null, AIUsageStatus.FAILED, context.error().getMessage());
    }

    @Override
    public void onResponse(EmbeddingModelResponseContext context) {
        logUsage(
                context.embeddingModel().modelName(),
                embeddingInputTokens(context.response().tokenUsage(), context.textSegments()),
                0,
                AIUsageStatus.SUCCESS,
                null
        );
    }

    @Override
    public void onError(EmbeddingModelErrorContext context) {
        logUsage(context.embeddingModel().modelName(), null, 0, AIUsageStatus.FAILED, context.error().getMessage());
    }


    // Helper

    // Lời gọi nằm ngoài AIUsageContext không có conversationType (bắt buộc) nên không ghi được;
    // cảnh báo để phát hiện luồng mới quên mở context.
    private void logUsage(String model, Integer inputTokens, Integer outputTokens, AIUsageStatus status, String errorMessage) {

        AIUsageContext usageContext = AIUsageContext.current().orElse(null);

        if (usageContext == null) {
            log.warn("AI call made outside an AIUsageContext, usage not logged (model {})", model);
            return;
        }

        aiUsageLogService.logAiUsage(AIUsageLogRequest.builder()
                .conversationType(usageContext.getConversationType())
                .conversationId(usageContext.getConversationId())
                .generationId(usageContext.getGenerationId())
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    // GoogleAiEmbeddingModel không gắn TokenUsage cho embedding, nên ước lượng theo số ký tự của từng
    // đoạn rồi cộng lại, để usage log vẫn có số token khác 0.
    private Integer embeddingInputTokens(TokenUsage tokenUsage, List<TextSegment> segments) {

        if (tokenUsage != null) {
            return tokenUsage.inputTokenCount();
        }

        return segments.stream()
                .mapToInt(segment -> Math.max(1, segment.text().length() / CHARS_PER_TOKEN))
                .sum();
    }
}
