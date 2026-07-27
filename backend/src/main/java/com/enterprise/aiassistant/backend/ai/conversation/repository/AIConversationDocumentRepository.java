package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AIConversationDocumentRepository extends JpaRepository<AIConversationDocument, Long> {

    // AIConversationDocument no longer has a back-reference, so navigate the join from AIConversation.
    @Query("SELECT acd.documentVersion.id FROM AIConversation c JOIN c.conversationDocuments acd WHERE c.id = :conversationId")
    List<Long> findDocumentVersionIdsByAiConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT acd FROM AIConversation c JOIN c.conversationDocuments acd " +
            "JOIN FETCH acd.documentVersion dv " +
            "JOIN FETCH dv.document " +
            "WHERE c.id = :conversationId")
    List<AIConversationDocument> findByAiConversationIdWithDocument(@Param("conversationId") Long conversationId);
}
