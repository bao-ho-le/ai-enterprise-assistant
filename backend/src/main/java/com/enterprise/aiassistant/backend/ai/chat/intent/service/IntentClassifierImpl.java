package com.enterprise.aiassistant.backend.ai.chat.intent.service;

import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.chat.intent.helper.IntentHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntentClassifierImpl implements IntentClassifier {

    // Phân loại intent chỉ là bước hỗ trợ routing: LLM lỗi hoặc trả giá trị lạ thì giữ
    // nguyên behavior cũ của conversation DOCUMENT_QA thay vì làm hỏng cả lượt chat.
    public static final Intent FALLBACK_INTENT = Intent.GENERAL_CHAT;

    private final ChatAssistant chatAssistant;
    private final LLMHelper llmHelper;
    private final AIMessageHelper messageHelper;
    private final IntentHelper intentHelper;

    @Override
    public Intent classify(String message) {

        messageHelper.validateContent(message);

        try {
            return intentHelper.parseIntent(llmHelper.call(() -> chatAssistant.classifyIntent(message)));

        } catch (LLMException e) {
            return FALLBACK_INTENT;
        }
    }


}
