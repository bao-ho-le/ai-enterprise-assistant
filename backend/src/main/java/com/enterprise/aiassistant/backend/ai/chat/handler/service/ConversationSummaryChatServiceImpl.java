package com.enterprise.aiassistant.backend.ai.chat.handler.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// User hỏi về chính cuộc hội thoại: chỉ dùng ConversationMemory, không embedding,
// không Qdrant, không DocumentContextService.
@Service
@RequiredArgsConstructor
public class ConversationSummaryChatServiceImpl implements ConversationSummaryChatService {

    private final ConversationMemoryService conversationMemoryService;
    private final ChatAssistant chatAssistant;
    private final LLMHelper llmHelper;

    private final AIConversationHelper aiConversationHelper;
    private final AIMessageHelper messageHelper;

    @Override
    @Transactional(readOnly = true)
    public String summarize(
            AIConversation conversation,
            String message
    ) {

        aiConversationHelper.validateConversationId(conversation.getId());
        messageHelper.validateContent(message);

        String conversationMemory = conversationMemoryService.buildMemoryContext(conversation.getId());

        return llmHelper.call(() -> chatAssistant.summarizeConversation(message, conversationMemory));
    }
}
