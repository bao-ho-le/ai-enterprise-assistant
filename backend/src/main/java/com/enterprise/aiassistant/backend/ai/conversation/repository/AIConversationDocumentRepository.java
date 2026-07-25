package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIConversationDocumentRepository
        extends JpaRepository<AIConversationDocument, Long> {

    List<AIConversationDocument> findByConversationIdOrderByAttachedAtAsc(Long conversationId);
}
