package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIMessageSourceRepository
        extends JpaRepository<AIMessageSource, Long> {

    /**
     * Lấy toàn bộ source của một assistant message.
     */
    List<AIMessageSource> findByMessageIdOrderByIdAsc(
            Long messageId
    );

    /**
     * Xóa toàn bộ source của một message.
     */
    void deleteByMessageId(
            Long messageId
    );

    /**
     * Kiểm tra message có source hay không.
     */
    boolean existsByMessageId(
            Long messageId
    );

    /**
     * Đếm số source của message.
     */
    long countByMessageId(
            Long messageId
    );
}