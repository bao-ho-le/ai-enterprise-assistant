package com.enterprise.aiassistant.backend.generated.repository;

import com.enterprise.aiassistant.backend.generated.entity.GenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRunRepository extends JpaRepository<GenerationRun, Long> {

    // No cascade from AIConversation anymore (unidirectional, child-owned) - hard delete needs this
    // explicitly, and must run before GeneratedContentRepository#deleteByAiConversationId since
    // generation_runs.generated_content_id references generated_content.
    void deleteByAiConversationId(Long aiConversationId);
}
