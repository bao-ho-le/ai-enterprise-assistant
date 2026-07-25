package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    /**
     * Lấy toàn bộ message theo thứ tự
     */
    Slice<AIMessage> findByConversationIdOrderByCreatedAtAsc(
            Long conversationId,
            Pageable pageable
    );

    /**
     * Lấy message cuối cùng của conversation.
     */
    Optional<AIMessage> findFirstByConversationIdOrderByCreatedAtDesc(
            Long conversationId
    );

    /**
     * Lấy message cuối cùng theo role.
     */
    Optional<AIMessage> findFirstByConversationIdAndRoleOrderByCreatedAtDesc(
            Long conversationId,
            AIMessageRole role
    );

    /**
     * Kiểm tra conversation có message hay chưa.
     */
    boolean existsByConversationId(
            Long conversationId
    );

    /**
     * Đếm số message trong conversation.
     */
    long countByConversationId(
            Long conversationId
    );
}