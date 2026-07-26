package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AIConversationRepository
        extends JpaRepository<AIConversation, Long>, AIConversationRepositoryCustom {

    Optional<AIConversation> findByIdAndStatus(Long conversationId, ConversationStatus status);

    boolean existsByIdAndStatus(Long conversationId, ConversationStatus status);
}
