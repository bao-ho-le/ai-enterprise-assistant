package com.enterprise.aiassistant.backend.ai.analytics.usage.listener;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.listener.EmbeddingModelResponseContext;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIUsageListenerTest {

    @Mock
    private AIUsageLogService aiUsageLogService;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private AIUsageListener listener;

    @Captor
    private ArgumentCaptor<AIUsageLogRequest> logCaptor;

    private final ChatRequest chatRequest = ChatRequest.builder()
            .messages(UserMessage.from("Xin chào"))
            .modelName("gemini-request-model")
            .build();

    // Mỗi test tự mở AIUsageContext bằng try-with-resources: ra khỏi khối try là context được đóng,
    // không rò sang test khác chạy trên cùng thread.

    @Test
    void onResponse_chatSucceeds_logsSuccessWithTokensAndContext() {
        ChatResponse chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("Chào bạn"))
                .modelName("gemini-response-model")
                .tokenUsage(new TokenUsage(10, 5))
                .build();

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.DOCUMENT_QA, 1L, null)) {
            listener.onResponse(new ChatModelResponseContext(chatResponse, chatRequest, ModelProvider.GOOGLE_AI_GEMINI, Map.of()));
        }

        verify(aiUsageLogService).logAiUsage(logCaptor.capture());
        AIUsageLogRequest log = logCaptor.getValue();
        assertThat(log.getConversationType()).isEqualTo(ConversationType.DOCUMENT_QA);
        assertThat(log.getConversationId()).isEqualTo(1L);
        assertThat(log.getModel()).isEqualTo("gemini-response-model");
        assertThat(log.getInputTokens()).isEqualTo(10);
        assertThat(log.getOutputTokens()).isEqualTo(5);
        assertThat(log.getStatus()).isEqualTo(AIUsageStatus.SUCCESS);
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void onResponse_responseWithoutModelName_usesRequestModel() {
        ChatResponse chatResponse = ChatResponse.builder().aiMessage(AiMessage.from("Chào bạn")).build();

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.DOCUMENT_QA, 1L, null)) {
            listener.onResponse(new ChatModelResponseContext(chatResponse, chatRequest, ModelProvider.GOOGLE_AI_GEMINI, Map.of()));
        }

        verify(aiUsageLogService).logAiUsage(logCaptor.capture());
        assertThat(logCaptor.getValue().getModel()).isEqualTo("gemini-request-model");
        // Không có TokenUsage thì để null, AIUsageLogService tự xử lý tiếp
        assertThat(logCaptor.getValue().getInputTokens()).isNull();
    }

    @Test
    void onError_chatFails_logsFailedWithErrorMessage() {
        RuntimeException error = new RuntimeException("Gemini quá tải");

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.REPORT_GENERATION, 2L, 9L)) {
            listener.onError(new ChatModelErrorContext(error, chatRequest, ModelProvider.GOOGLE_AI_GEMINI, Map.of()));
        }

        verify(aiUsageLogService).logAiUsage(logCaptor.capture());
        AIUsageLogRequest log = logCaptor.getValue();
        assertThat(log.getStatus()).isEqualTo(AIUsageStatus.FAILED);
        assertThat(log.getErrorMessage()).isEqualTo("Gemini quá tải");
        assertThat(log.getModel()).isEqualTo("gemini-request-model");
        assertThat(log.getGenerationId()).isEqualTo(9L);
    }

    // Gemini không trả số token cho embedding: ước lượng ~4 ký tự/token cho từng đoạn, tối thiểu 1
    @Test
    void onResponse_embeddingWithoutTokenUsage_estimatesTokensFromText() {
        when(embeddingModel.modelName()).thenReturn("gemini-embedding-001");
        EmbeddingModelResponseContext context = EmbeddingModelResponseContext.builder()
                .embeddingModel(embeddingModel)
                .response(Response.from(List.of()))
                .textSegments(List.of(
                        TextSegment.from("a".repeat(40)),   // 40 / 4 = 10 token
                        TextSegment.from("ab")              // 2 / 4 = 0, nâng lên tối thiểu 1 token
                ))
                .attributes(Map.of())
                .build();

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.DOCUMENT_INDEXING, null, null)) {
            listener.onResponse(context);
        }

        verify(aiUsageLogService).logAiUsage(logCaptor.capture());
        assertThat(logCaptor.getValue().getModel()).isEqualTo("gemini-embedding-001");
        assertThat(logCaptor.getValue().getInputTokens()).isEqualTo(11);
        assertThat(logCaptor.getValue().getOutputTokens()).isZero();
    }

    // Lời gọi AI không nằm trong AIUsageContext thì không biết thuộc luồng nào, nên bỏ qua (chỉ ghi warning)
    @Test
    void onResponse_outsideUsageContext_logsNothing() {
        ChatResponse chatResponse = ChatResponse.builder().aiMessage(AiMessage.from("Chào bạn")).build();

        listener.onResponse(new ChatModelResponseContext(chatResponse, chatRequest, ModelProvider.GOOGLE_AI_GEMINI, Map.of()));

        verifyNoInteractions(aiUsageLogService);
    }
}
