package com.example.ai_document_assistant.document.repository;

import com.example.ai_document_assistant.document.dto.DocumentResponse;
import com.example.ai_document_assistant.document.entity.Document;
import com.example.ai_document_assistant.document.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
            SELECT new com.example.ai_document_assistant.document.dto.DocumentResponse(
                d.id, d.title, dv.createdAt, d.documentType, dv.versionNumber, f.fileSize, dv.status
            )
            FROM Document d
            JOIN DocumentVersion dv ON dv.id = d.currentVersionId
            JOIN FileEntity f ON f.id = dv.fileId
            WHERE d.status = :status
            """)
    Page<DocumentResponse> findAllByStatus(@Param("status") DocumentStatus status, Pageable pageable);
}
