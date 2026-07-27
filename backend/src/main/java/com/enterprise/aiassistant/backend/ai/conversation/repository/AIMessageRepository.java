package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    // Dùng cho API GET /ai-conversations/{id}/messages — đọc theo thứ tự thời gian tăng dần
    Page<AIMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    // Dùng cho GET /ai-conversations/{id} — lấy N message gần nhất (giảm dần rồi đảo lại ở service)
    Page<AIMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    // No cascade from AIConversation anymore (unidirectional, child-owned) - hard delete needs this explicitly.
    void deleteByConversationId(Long conversationId);
}
