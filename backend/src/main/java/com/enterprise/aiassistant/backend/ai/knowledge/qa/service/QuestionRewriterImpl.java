package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Chạy trước Document QA retrieval: dùng ConversationMemory hiện có (không query lại
// AIMessage) để resolve reference trong câu hỏi hiện tại, KHÔNG dùng để trả lời câu hỏi.
@Service
@RequiredArgsConstructor
public class QuestionRewriterImpl implements QuestionRewriter {

    private final ConversationMemoryService conversationMemoryService;
    private final ChatAssistant chatAssistant;
    private final LLMHelper llmHelper;

    private final AIConversationHelper aiConversationHelper;
    private final AIMessageHelper messageHelper;

    @Override
    public String rewrite(
            AIConversation conversation,
            String userMessage
    ) {

        validateRewriteRequest(conversation, userMessage);

        String conversationMemory = conversationMemoryService.buildMemoryContext(conversation.getId());

        try {
            String rewrittenQuestion = llmHelper.call(() -> chatAssistant.rewriteQuestion(userMessage, conversationMemory));

            return normalize(rewrittenQuestion, userMessage);

        } catch (LLMException e) {
            // Rewrite chỉ hỗ trợ retrieval, không phải bước bắt buộc: lỗi thì dùng nguyên
            // văn câu hỏi, Document QA vẫn chạy như hành vi cũ thay vì làm cả lượt chat lỗi.
            return userMessage;
        }
    }


    // Helper

    // Model đôi khi bọc câu trả lời trong dấu ngoặc kép; chỉ chuẩn hoá whitespace/quote
    // bao ngoài, không biến đổi nội dung câu hỏi.
    private String normalize(String rewrittenQuestion, String userMessage) {

        if (rewrittenQuestion == null || rewrittenQuestion.isBlank()) {
            return userMessage;
        }

        String normalized = rewrittenQuestion.trim();

        if (normalized.length() > 1 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        return normalized.isBlank() ? userMessage : normalized;
    }

    private void validateRewriteRequest(
            AIConversation conversation,
            String userMessage
    ) {

        if (conversation == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        aiConversationHelper.validateConversationId(conversation.getId());
        messageHelper.validateContent(userMessage);
    }
}
