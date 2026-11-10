package com.enterprise.aiassistant.backend.ai.chat.handler.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationSummaryChatServiceImplTest {

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @Mock
    private ChatAssistant chatAssistant;

    @Mock
    private LLMHelper llmHelper;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Spy
    private AIMessageHelper messageHelper = new AIMessageHelper();

    @InjectMocks
    private ConversationSummaryChatServiceImpl service;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    // Tóm tắt hội thoại chỉ dựa vào memory của chính hội thoại, không đụng tới tài liệu
    @Test
    void summarize_validMessage_passesConversationMemoryToLlm() {
        when(conversationMemoryService.buildMemoryContext(1L)).thenReturn("USER: Hỏi A\nASSISTANT: Trả lời A");
        when(llmHelper.call(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(chatAssistant.summarizeConversation("Nãy giờ mình nói gì?", "USER: Hỏi A\nASSISTANT: Trả lời A"))
                .thenReturn("Bạn đã hỏi về A");

        assertThat(service.summarize(conversation, "Nãy giờ mình nói gì?")).isEqualTo("Bạn đã hỏi về A");
    }

    @Test
    void summarize_blankMessage_throwsWithoutReadingMemory() {
        assertThatThrownBy(() -> service.summarize(conversation, "  "))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);

        verifyNoInteractions(conversationMemoryService, llmHelper);
    }
}
