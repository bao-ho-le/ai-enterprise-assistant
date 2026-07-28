package com.enterprise.aiassistant.backend.generated.repository;

import com.enterprise.aiassistant.backend.generated.entity.GenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenerationRunRepository extends JpaRepository<GenerationRun, Long> {

    void deleteByAiConversationId(Long aiConversationId);

    Optional<GenerationRun> findFirstByAiConversationIdOrderByCreatedAtDesc(Long aiConversationId);
}
