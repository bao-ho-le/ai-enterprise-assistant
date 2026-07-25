package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AIMessageSourceRepository extends JpaRepository<AIMessageSource, Long> {

    List<AIMessageSource> findByMessageIdIn(Collection<Long> messageIds);
}
