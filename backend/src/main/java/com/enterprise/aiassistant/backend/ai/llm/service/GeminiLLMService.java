package com.enterprise.aiassistant.backend.ai.llm.service;

import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.llm.mapper.GeminiResponseMapper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiLLMService implements LLMService {

    private final ChatModel chatModel;
    private final GeminiResponseMapper geminiResponseMapper;
    private final LLMHelper llmHelper;

    @Override
    public LLMResponse generate(LLMRequest request) {
        llmHelper.validateGenerateRequest(request);

        try {
            ChatResponse response = chatModel.chat(UserMessage.from(request.getPrompt()));
            return geminiResponseMapper.mapToLLMResponse(response, getModelName());
        } catch (Exception e) {
            // Bắt mọi lỗi từ langchain4j (network, quota, timeout...), không phân loại chi tiết
            throw new LLMException(ErrorCode.LLM_GENERATION_FAILED, ErrorCode.LLM_GENERATION_FAILED.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return "gemini-3.1-flash-lite";
    }
}
