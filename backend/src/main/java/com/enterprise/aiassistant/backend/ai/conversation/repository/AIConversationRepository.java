package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {

    // ponytail: assumes ai_messages/generated_content use "ai_conversation_id" as FK column,
    // matching ai_conversation_documents' naming - no entity exists for these tables to verify against.
    @Modifying
    @Query(value = "DELETE FROM ai_messages WHERE ai_conversation_id = :conversationId", nativeQuery = true)
    void deleteMessagesByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query(value = "DELETE FROM generated_content WHERE ai_conversation_id = :conversationId", nativeQuery = true)
    void deleteGeneratedContentByConversationId(@Param("conversationId") Long conversationId);
}
