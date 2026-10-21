package com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import dev.langchain4j.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Supplier;

@Component
public class LLMHelper {

    @Value("${gemini.llm-model.model-name}")
    private String modelName;

    // Tên model ghi vào AI usage log, kể cả khi lời gọi model thất bại.
    public String getModelName() {
        return modelName;
    }

    // Mọi lời gọi Gemini (ChatAssistant hoặc ChatModel) đi qua đây để lỗi của LangChain4j thành
    // LLMException: intent/rewrite/nén memory bắt LLMException để fallback, lỗi vượt token
    // trả về LLM_INPUT_TOO_LARGE.
    public <T> T call(Supplier<T> llmCall) {
        try {
            return llmCall.get();
        } catch (InvalidRequestException e) {
            if (isInputTooLarge(e)) {
                throw new LLMException(ErrorCode.LLM_INPUT_TOO_LARGE, e);
            }
            throw new LLMException(ErrorCode.LLM_GENERATION_FAILED, ErrorCode.LLM_GENERATION_FAILED.getMessage(), e);
        } catch (Exception e) {
            // Bắt mọi lỗi khác từ langchain4j (network, quota, timeout...), không phân loại chi tiết
            throw new LLMException(ErrorCode.LLM_GENERATION_FAILED, ErrorCode.LLM_GENERATION_FAILED.getMessage(), e);
        }
    }

    // Gemini không có mã lỗi riêng cho "vượt token" - mọi request không hợp lệ đều trả
    // HTTP 400 (InvalidRequestException), nên phải soi message mới phân biệt được.
    // "token count" khớp cả 2 dạng message thật của Gemini:
    // - "The input token count (N) exceeds the maximum number of tokens allowed (M)."
    // - "...the input token count is N but model only supports up to M..."
    private boolean isInputTooLarge(InvalidRequestException e) {
        String message = e.getMessage();

        if (message == null) {
            return false;
        }

        message = message.toLowerCase(Locale.ROOT);

        return message.contains("token count");
    }
}
