package com.enterprise.aiassistant.backend.ai.chat.message.helper;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIMessageHelperTest {

    private final AIMessageHelper helper = new AIMessageHelper();

    // ===================== validateConversationId =====================

    @Test
    void validateConversationId_null_throwsConversationIdRequired() {
        assertThatThrownBy(() -> helper.validateConversationId(null))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_ID_REQUIRED);
    }

    @Test
    void validateConversationId_hasValue_doesNotThrow() {
        assertThatCode(() -> helper.validateConversationId(1L)).doesNotThrowAnyException();
    }

    // ===================== validateChatConversationType =====================

    @Test
    void validateChatConversationType_documentQa_doesNotThrow() {
        assertThatCode(() -> helper.validateChatConversationType(ConversationType.DOCUMENT_QA))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = ConversationType.class, names = "DOCUMENT_QA", mode = EnumSource.Mode.EXCLUDE)
    void validateChatConversationType_otherType_throwsNotChat(ConversationType type) {
        assertThatThrownBy(() -> helper.validateChatConversationType(type))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_TYPE_NOT_CHAT);
    }

    // ===================== validateRequest =====================

    @Test
    void validateRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateRequest(null))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateRequest_blankContent_throwsContentRequired() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("  ");

        assertThatThrownBy(() -> helper.validateRequest(request))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);
    }

    @Test
    void validateRequest_valid_doesNotThrow() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hợp đồng này hết hạn khi nào?");

        assertThatCode(() -> helper.validateRequest(request)).doesNotThrowAnyException();
    }

    // ===================== validateContent =====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void validateContent_blank_throwsContentRequired(String content) {
        assertThatThrownBy(() -> helper.validateContent(content))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);
    }

    @Test
    void validateContent_over10000Chars_throwsContentTooLong() {
        String content = "a".repeat(10_001);

        assertThatThrownBy(() -> helper.validateContent(content))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_TOO_LONG);
    }

    @Test
    void validateContent_exactly10000Chars_doesNotThrow(){
        String content = "a".repeat(10000);

        assertThatCode(() -> helper.validateContent(content)).doesNotThrowAnyException();
    }

    // ===================== validateMessageId =====================

    @Test
    void validateMessageId_null_throwsMessageIdRequired() {
        assertThatThrownBy(() -> helper.validateMessageId(null))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_ID_REQUIRED);
    }

    @Test
    void validateMessageId_hasValue_doesNotThrow() {
        assertThatCode(() -> helper.validateMessageId(1L)).doesNotThrowAnyException();
    }
}
