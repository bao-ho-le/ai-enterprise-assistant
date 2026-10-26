package com.enterprise.aiassistant.backend.ai.chat.intent.service;

import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntentClassifierImpl implements IntentClassifier {

    // Phân loại intent chỉ là bước hỗ trợ routing: LLM lỗi hoặc trả giá trị không đổi được sang enum
    // (LLMHelper đổi thành LLMException) thì về GENERAL_CHAT thay vì làm hỏng cả lượt chat.
    private static final Intent FALLBACK_INTENT = Intent.GENERAL_CHAT;

    private final ChatAssistant chatAssistant;
    private final LLMHelper llmHelper;
    private final AIMessageHelper messageHelper;

    @Override
    public Intent classify(String message) {

        messageHelper.validateContent(message);

        try {
            return llmHelper.call(() -> chatAssistant.classifyIntent(message));

        } catch (LLMException e) {
            return FALLBACK_INTENT;
        }
    }


}
