package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentTextRepository extends JpaRepository<DocumentText, Long> {

    Optional<DocumentText> findByDocumentVersion(DocumentVersion documentVersion);

    Optional<DocumentText> findByDocumentVersionId(Long documentVersionId);

    boolean existsByDocumentVersionId(Long documentVersionId);

}