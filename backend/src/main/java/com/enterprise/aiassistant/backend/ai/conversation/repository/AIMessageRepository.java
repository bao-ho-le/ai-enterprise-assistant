package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {
}
