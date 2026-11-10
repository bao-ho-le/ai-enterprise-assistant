package com.enterprise.aiassistant.backend.ai.chat.intent.service;

import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
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
class IntentClassifierImplTest {

    @Mock
    private ChatAssistant chatAssistant;

    @Mock
    private LLMHelper llmHelper;

    // Helper thật để test được trường hợp message rỗng
    @Spy
    private AIMessageHelper messageHelper = new AIMessageHelper();

    @InjectMocks
    private IntentClassifierImpl classifier;

    // thenAnswer chạy lambda truyền vào llmHelper.call, nên chatAssistant.classifyIntent thật sự được gọi
    @Test
    void classify_llmReturnsIntent_returnsThatIntent() {
        when(llmHelper.call(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(chatAssistant.classifyIntent("Tóm tắt tài liệu giúp tôi")).thenReturn(Intent.SUMMARY);

        assertThat(classifier.classify("Tóm tắt tài liệu giúp tôi")).isEqualTo(Intent.SUMMARY);
    }

    // LLMHelper đổi mọi lỗi của LLM (kể cả trả giá trị không phải enum) thành LLMException
    @Test
    void classify_llmFails_fallsBackToGeneralChat() {
        when(llmHelper.call(any())).thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED));

        assertThat(classifier.classify("Xin chào")).isEqualTo(Intent.GENERAL_CHAT);
    }

    @Test
    void classify_blankMessage_throwsWithoutCallingLlm() {
        assertThatThrownBy(() -> classifier.classify("   "))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);

        verifyNoInteractions(llmHelper, chatAssistant);
    }
}
