package com.enterprise.aiassistant.backend.ai.chat.handler.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Câu hỏi không cần tới tài liệu đính kèm: gọi thẳng LLM, không embedding/Qdrant/document context.
@Service
@RequiredArgsConstructor
public class GeneralChatServiceImpl implements GeneralChatService {

    private final ChatAssistant chatAssistant;
    private final LLMHelper llmHelper;
    private final AIMessageHelper messageHelper;

    @Override
    public String answer(String message) {

        messageHelper.validateContent(message);

        return llmHelper.call(() -> chatAssistant.generalChat(message));
    }
}
