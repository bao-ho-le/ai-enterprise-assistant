package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {

    Optional<AIConversation> findByIdAndDeletedFalse(Long conversationId);
}
