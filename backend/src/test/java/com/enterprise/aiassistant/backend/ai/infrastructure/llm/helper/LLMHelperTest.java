package com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import dev.langchain4j.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LLMHelperTest {

    private final LLMHelper helper = new LLMHelper();

    @Test
    void call_llmSucceeds_returnsResult() {
        assertThat(helper.call(() -> "Chào bạn")).isEqualTo("Chào bạn");
    }

    // Gemini không có mã lỗi riêng cho "vượt token", phải nhận biết qua nội dung message
    @Test
    void call_inputTokenLimitExceeded_throwsInputTooLarge() {
        InvalidRequestException error = new InvalidRequestException(
                "The input token count (2000000) exceeds the maximum number of tokens allowed (1048576).");

        assertThatThrownBy(() -> helper.call(() -> {
            throw error;
        }))
                .isInstanceOf(LLMException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LLM_INPUT_TOO_LARGE)
                .hasCause(error);
    }

    @Test
    void call_otherInvalidRequest_throwsGenerationFailed() {
        InvalidRequestException error = new InvalidRequestException("API key not valid");

        assertThatThrownBy(() -> helper.call(() -> {
            throw error;
        }))
                .isInstanceOf(LLMException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LLM_GENERATION_FAILED)
                .hasCause(error);
    }

    @Test
    void call_unexpectedError_throwsGenerationFailed() {
        RuntimeException error = new RuntimeException("Connection reset");

        assertThatThrownBy(() -> helper.call(() -> {
            throw error;
        }))
                .isInstanceOf(LLMException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LLM_GENERATION_FAILED)
                .hasCause(error);
    }
}
