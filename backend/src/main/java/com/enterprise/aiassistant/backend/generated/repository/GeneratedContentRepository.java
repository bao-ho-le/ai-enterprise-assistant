
package com.enterprise.aiassistant.backend.generated.repository;

import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedContentRepository
        extends JpaRepository<GeneratedContent, Long> {

    Slice<GeneratedContent> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    Slice<GeneratedContent> findByGeneratedTypeOrderByCreatedAtDesc(
            GeneratedDocumentType generatedType,
            Pageable pageable
    );

    List<GeneratedContent> findByAiConversationIdOrderByCreatedAtDesc(
            Long aiConversationId
    );

    Optional<GeneratedContent>
    findFirstByAiConversationIdAndGeneratedTypeOrderByCreatedAtDesc(
            Long aiConversationId,
            GeneratedDocumentType generatedType
    );

    boolean existsByAiConversationId(
            Long aiConversationId
    );
}

