package com.enterprise.aiassistant.backend.ai.chat.handler.service;

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
class GeneralChatServiceImplTest {

    @Mock
    private ChatAssistant chatAssistant;

    @Mock
    private LLMHelper llmHelper;

    @Spy
    private AIMessageHelper messageHelper = new AIMessageHelper();

    @InjectMocks
    private GeneralChatServiceImpl service;

    @Test
    void answer_validMessage_returnsLlmAnswer() {
        when(llmHelper.call(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(chatAssistant.generalChat("Xin chào")).thenReturn("Chào bạn");

        assertThat(service.answer("Xin chào")).isEqualTo("Chào bạn");
    }

    @Test
    void answer_blankMessage_throwsWithoutCallingLlm() {
        assertThatThrownBy(() -> service.answer(""))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);

        verifyNoInteractions(llmHelper);
    }
}
