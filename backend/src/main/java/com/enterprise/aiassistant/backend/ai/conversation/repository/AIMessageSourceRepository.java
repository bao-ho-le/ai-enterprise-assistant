package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIMessageSourceRepository extends JpaRepository<AIMessageSource, Long> {
}
