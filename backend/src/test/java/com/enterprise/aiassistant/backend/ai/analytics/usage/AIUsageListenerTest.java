package com.enterprise.aiassistant.backend.ai.analytics.usage;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.listener.AIUsageContext;
import com.enterprise.aiassistant.backend.ai.analytics.usage.listener.AIUsageListener;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AIUsageListener gắn vào ChatModel/EmbeddingModel giả (không gọi Gemini): mỗi lượt gọi trong AIUsageContext
// thành đúng 1 lần logAiUsage với loại/conversation/generation/token/trạng thái; ngoài context thì không ghi;
// context lồng nhau trả lại context ngoài khi đóng.
class AIUsageListenerTest {

    private final AIUsageLogService aiUsageLogService = mock(AIUsageLogService.class);
    private final AIUsageListener listener = new AIUsageListener(aiUsageLogService);

    private boolean failChat;

    private final ChatModel chatModel = new ChatModel() {
        @Override
        public ChatResponse doChat(ChatRequest request) {
            if (failChat) {
                throw new RuntimeException("quota exceeded");
            }
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("ok"))
                    .modelName("gemini-test")
                    .tokenUsage(new TokenUsage(11, 22))
                    .build();
        }

        @Override
        public ChatRequestParameters defaultRequestParameters() {
            return ChatRequestParameters.builder().modelName("gemini-test").build();
        }

        @Override
        public List<ChatModelListener> listeners() {
            return List.of(listener);
        }
    };

    private final EmbeddingModel embeddingModel = new EmbeddingModel() {
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            return Response.from(segments.stream().map(segment -> Embedding.from(new float[]{1f})).toList());
        }

        @Override
        public String modelName() {
            return "embedding-test";
        }
    }.addListener(listener);

    @Test
    void chatCallsAreLoggedWithTheOpenContext() {

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.DOCUMENT_QA, 9L, null)) {
            chatModel.chat("Xin chào");
        }

        AIUsageLogRequest request = loggedRequest();
        assertEquals(ConversationType.DOCUMENT_QA, request.getConversationType());
        assertEquals(9L, request.getConversationId());
        assertNull(request.getGenerationId());
        assertEquals("gemini-test", request.getModel());
        assertEquals(11, request.getInputTokens());
        assertEquals(22, request.getOutputTokens());
        assertEquals(AIUsageStatus.SUCCESS, request.getStatus());
    }

    @Test
    void failedChatCallsAreLoggedAsFailed() {

        failChat = true;

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.EMAIL_GENERATION, 3L, 5L)) {
            assertThrows(RuntimeException.class, () -> chatModel.chat("Viết email"));
        }

        AIUsageLogRequest request = loggedRequest();
        assertEquals(5L, request.getGenerationId());
        assertEquals("gemini-test", request.getModel());
        assertNull(request.getInputTokens());
        assertEquals(AIUsageStatus.FAILED, request.getStatus());
        assertTrue(request.getErrorMessage().contains("quota exceeded"));
    }

    // Gemini không trả token cho embedding: ước lượng ~4 ký tự/token, tối thiểu 1 token mỗi đoạn.
    @Test
    void embeddingTokensAreEstimatedFromText() {

        try (AIUsageContext ignored = AIUsageContext.open(ConversationType.SEMANTIC_SEARCH, null, null)) {
            embeddingModel.embedAll(List.of(TextSegment.from("12345678"), TextSegment.from("a")));
        }

        AIUsageLogRequest request = loggedRequest();
        assertEquals(ConversationType.SEMANTIC_SEARCH, request.getConversationType());
        assertEquals("embedding-test", request.getModel());
        assertEquals(3, request.getInputTokens());
        assertEquals(0, request.getOutputTokens());
    }

    @Test
    void callsOutsideAContextAreNotLoggedAndNestedContextsRestore() {

        chatModel.chat("ngoài context");
        verifyNoInteractions(aiUsageLogService);

        try (AIUsageContext outer = AIUsageContext.open(ConversationType.DOCUMENT_QA, 1L, null)) {
            try (AIUsageContext inner = AIUsageContext.open(ConversationType.REPORT_GENERATION, 2L, 7L)) {
                assertEquals(7L, AIUsageContext.current().orElseThrow().getGenerationId());
            }
            assertEquals(1L, AIUsageContext.current().orElseThrow().getConversationId());
        }
        assertTrue(AIUsageContext.current().isEmpty());
    }

    private AIUsageLogRequest loggedRequest() {
        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService, times(1)).logAiUsage(captor.capture());
        return captor.getValue();
    }
}
