package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIMessageSourceRepository
        extends JpaRepository<AIMessageSource, Long> {

    // Lấy toàn bộ source của một assistant message.
    List<AIMessageSource> findByMessageIdOrderByIdAsc(
            Long messageId
    );

    void deleteByMessageId(
            Long messageId
    );

    boolean existsByMessageId(
            Long messageId
    );

    long countByMessageId(
            Long messageId
    );

    void deleteByMessage_ConversationId(Long conversationId);
}