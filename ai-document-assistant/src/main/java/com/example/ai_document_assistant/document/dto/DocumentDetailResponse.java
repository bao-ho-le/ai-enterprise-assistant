package com.example.ai_document_assistant.document.dto;

import com.example.ai_document_assistant.document.enums.DocumentStatus;
import com.example.ai_document_assistant.document.enums.DocumentType;
import com.example.ai_document_assistant.document.enums.VersionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDetailResponse(
        DocumentInfo documentInfo,
        CurrentVersionInfo currentVersion,
        List<VersionHistoryItem> versionHistory,
        AdvancedInfo advancedInfo
) {

    public record DocumentInfo(
            String title,
            String description,
            DocumentStatus status,
            DocumentType documentType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CurrentVersionInfo(
            Integer versionNumber,
            VersionStatus versionStatus,
            String fileName,
            String extension,
            Long fileSize,
            LocalDateTime uploadedDate
    ) {
    }

    public record VersionHistoryItem(
            Integer versionNumber,
            String fileName,
            String changeNote,
            VersionStatus status,
            LocalDateTime createdAt
    ) {
    }

    public record AdvancedInfo(
            String storageProvider,
            String bucketName,
            String objectKey,
            String checksum
    ) {
    }
}
