package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AIConversationDocumentRepository
        extends JpaRepository<AIConversationDocument, Long> {

    @Query("SELECT acd FROM AIConversationDocument acd " +
            "JOIN FETCH acd.documentVersion dv " +
            "JOIN FETCH dv.document " +
            "WHERE acd.conversation.id = :conversationId " +
            "ORDER BY acd.attachedAt ASC")
    List<AIConversationDocument> findByConversationIdOrderByAttachedAtAsc(@Param("conversationId") Long conversationId);
}
