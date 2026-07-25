package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIConversationRepository
        extends JpaRepository<AIConversation, Long>, AIConversationRepositoryCustom {
}
