package com.enterprise.aiassistant.backend.ai.chat.memory.helper;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationMemoryHelperTest {

    // Helper này cần 2 helper khác, cả hai đều không có dependency nên tạo thật bằng new, không cần mock
    private final ConversationMemoryHelper helper =
            new ConversationMemoryHelper(new AIConversationHelper(), new AIMessageHelper());

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    // ===================== validateTurn =====================

    @Test
    void validateTurn_nullConversation_throwsConversationNotFound() {
        assertThatThrownBy(() -> helper.validateTurn(null, "Câu hỏi", "Câu trả lời"))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void validateTurn_conversationWithoutId_throwsConversationIdRequired() {
        AIConversation withoutId = AIConversation.builder().build();

        assertThatThrownBy(() -> helper.validateTurn(withoutId, "Câu hỏi", "Câu trả lời"))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_ID_REQUIRED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateTurn_blankAssistantResponse_throwsContentRequired(String assistantResponse) {
        assertThatThrownBy(() -> helper.validateTurn(conversation, "Câu hỏi", assistantResponse))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);
    }

    @Test
    void validateTurn_validTurn_doesNotThrow() {
        assertThatCode(() -> helper.validateTurn(conversation, "Câu hỏi", "Câu trả lời"))
                .doesNotThrowAnyException();
    }

    // ===================== appendTurn =====================

    @Test
    void appendTurn_nullPendingContext_startsNewContext() {
        String result = helper.appendTurn(null, "Xin chào", "Chào bạn");

        assertThat(result).isEqualTo("USER:\nXin chào\n\nASSISTANT:\nChào bạn\n\n");
    }

    @Test
    void appendTurn_existingPendingContext_appendsTrimmedTurn() {
        String pending = "USER:\nCâu cũ\n\nASSISTANT:\nĐáp cũ\n\n";

        String result = helper.appendTurn(pending, "  Câu mới  ", "  Đáp mới  ");

        assertThat(result).isEqualTo(pending + "USER:\nCâu mới\n\nASSISTANT:\nĐáp mới\n\n");
    }

    // ===================== calculateCharacterCount =====================

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "NULL, NULL, 0",
            "NULL, abc,  3",
            "ab,   abc,  5"
    })
    void calculateCharacterCount_treatsNullAsEmpty(String summarized, String pending, int expected) {
        assertThat(helper.calculateCharacterCount(summarized, pending)).isEqualTo(expected);
    }

    // ===================== needsSummarization =====================

    @Test
    void needsSummarization_belowLimit_returnsFalse() {
        assertThat(helper.needsSummarization(15_999)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {16_000, 20_000})
    void needsSummarization_atOrAboveLimit_returnsTrue(int characterCount) {
        // Ngưỡng dùng >= nên đúng 16.000 ký tự đã phải nén
        assertThat(helper.needsSummarization(characterCount)).isTrue();
    }

    // ===================== buildContext =====================

    @Test
    void buildContext_bothEmpty_returnsEmptyContext() {
        assertThat(helper.buildContext(null, "   ")).isEqualTo(helper.emptyContext());
    }

    @Test
    void buildContext_bothParts_putsSummaryBeforeRecentTurns() {
        String result = helper.buildContext("Tóm tắt cũ", "USER:\nHỏi");

        assertThat(result).isEqualTo("Summary of earlier turns:\nTóm tắt cũ\n\nRecent turns:\nUSER:\nHỏi");
    }

    @Test
    void buildContext_onlyPending_returnsRecentTurnsOnly() {
        assertThat(helper.buildContext("", "USER:\nHỏi")).isEqualTo("Recent turns:\nUSER:\nHỏi");
    }

    // ===================== orNone =====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void orNone_blank_returnsNone(String context) {
        assertThat(helper.orNone(context)).isEqualTo("None.");
    }

    @Test
    void orNone_hasValue_returnsSameValue() {
        assertThat(helper.orNone("Tóm tắt")).isEqualTo("Tóm tắt");
    }
}
