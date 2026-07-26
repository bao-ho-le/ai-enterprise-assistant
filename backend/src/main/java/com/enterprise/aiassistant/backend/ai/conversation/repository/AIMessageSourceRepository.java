package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AIMessageSourceRepository extends JpaRepository<AIMessageSource, Long> {

    @Query("SELECT s FROM AIMessageSource s " +
            "JOIN FETCH s.documentChunk dc " +
            "JOIN FETCH dc.documentVersion dv " +
            "JOIN FETCH dv.document " +
            "WHERE s.message.id IN :messageIds")
    List<AIMessageSource> findByMessageIdIn(@Param("messageIds") Collection<Long> messageIds);
}
