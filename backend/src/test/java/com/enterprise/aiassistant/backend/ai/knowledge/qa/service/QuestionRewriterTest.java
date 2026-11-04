package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// LLMHelper bị mock nên lambda gọi model bên trong không chạy: test chỉ quyết định
// "model trả gì" (thenReturn) hoặc "model lỗi" (thenThrow), rồi kiểm tra rewriter xử lý kết quả đó.
@ExtendWith(MockitoExtension.class)
class QuestionRewriterTest {

    private static final String QUESTION = "Nó hết hạn khi nào?";

    @Mock
    private ChatModel chatModel;

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @Mock
    private LLMHelper llmHelper;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Mock
    private AIMessageHelper messageHelper;

    @InjectMocks
    private QuestionRewriter rewriter;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    @Test
    void rewrite_nullConversation_throwsConversationNotFound() {
        assertThatThrownBy(() -> rewriter.rewrite(null, QUESTION))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_NOT_FOUND);

        verifyNoInteractions(llmHelper);
    }

    @Test
    void rewrite_invalidContent_doesNotCallLlm() {
        doThrow(new ConversationException(ErrorCode.MESSAGE_CONTENT_REQUIRED))
                .when(messageHelper).validateContent(" ");

        assertThatThrownBy(() -> rewriter.rewrite(conversation, " "))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);

        verifyNoInteractions(conversationMemoryService, llmHelper);
    }

    @Test
    void rewrite_modelReturnsQuestion_returnsRewrittenQuestion() {
        stubMemory();

        // Trả về kiểu List<Query> do rewrite thật gọi transform() sẽ trả về Collection<Query>
        when(llmHelper.call(any())).thenReturn(List.of(Query.from("Hợp đồng ABC hết hạn khi nào?")));

        assertThat(rewriter.rewrite(conversation, QUESTION)).isEqualTo("Hợp đồng ABC hết hạn khi nào?");
    }

    @Test
    void rewrite_quotedAnswer_stripsQuotes() {
        stubMemory();
        when(llmHelper.call(any())).thenReturn(List.of(Query.from("  \"Hợp đồng ABC hết hạn khi nào?\"  ")));

        assertThat(rewriter.rewrite(conversation, QUESTION)).isEqualTo("Hợp đồng ABC hết hạn khi nào?");
    }

    @Test
    void rewrite_onlyQuotes_returnsOriginalQuestion() {
        stubMemory();
        // Bỏ cặp ngoặc kép xong thì không còn gì, phải dùng lại câu gốc
        when(llmHelper.call(any())).thenReturn(List.of(Query.from("\"\"")));

        assertThat(rewriter.rewrite(conversation, QUESTION)).isEqualTo(QUESTION);
    }

    // TODO (bạn tự viết): rewrite_llmFails_returnsOriginalQuestion
    // Model lỗi: llmHelper.call(...) ném LLMException → rewriter KHÔNG được ném lỗi ra ngoài,
    // mà trả về nguyên văn câu hỏi gốc (QUESTION).
    // Gợi ý: gọi stubMemory(), rồi when(llmHelper.call(any())).thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED)).
    @Test
    void rewrite_llmFails_returnsOriginalQuestion() {
        stubMemory();
        when(llmHelper.call(any())).thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED));

        assertThat(rewriter.rewrite(conversation, QUESTION)).isEqualTo(QUESTION);
    }

    // ===================== test data =====================

    // Mock trả null nếu không stub, mà rewriter đưa memory vào UserMessage (không nhận null)
    private void stubMemory() {
        when(conversationMemoryService.buildMemoryContext(1L)).thenReturn("No previous conversation context.");
    }
}
