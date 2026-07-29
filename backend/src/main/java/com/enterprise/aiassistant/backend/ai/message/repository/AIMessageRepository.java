package com.enterprise.aiassistant.backend.ai.message.repository;

import com.enterprise.aiassistant.backend.ai.message.entity.AIMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    // Lấy toàn bộ message theo thứ tự thời gian tăng dần
    Slice<AIMessage> findByConversationIdOrderByCreatedAtAsc(
            Long conversationId,
            Pageable pageable
    );

    long countByConversationId(Long conversationId);

    Optional<AIMessage> findByIdAndConversationId(
            Long messageId,
            Long conversationId
    );

    void deleteByConversationId(Long conversationId);
}

