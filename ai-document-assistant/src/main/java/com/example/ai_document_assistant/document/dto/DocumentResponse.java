package com.example.ai_document_assistant.document.dto;

import com.example.ai_document_assistant.document.enums.DocumentType;
import com.example.ai_document_assistant.document.enums.VersionStatus;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String fileName,
        LocalDateTime uploadTime,
        DocumentType documentType,
        Integer currentVersion,
        Long size,
        VersionStatus status
) {
}
