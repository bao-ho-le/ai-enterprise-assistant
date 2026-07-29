package com.enterprise.aiassistant.backend.ai.qa.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessage;

public interface DocumentQAService {

    AIMessage answer(AIConversation conversation, String question);
}
