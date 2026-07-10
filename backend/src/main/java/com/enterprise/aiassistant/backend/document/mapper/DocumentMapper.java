package com.enterprise.aiassistant.backend.document.mapper;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public Document toDocument(DocumentUploadRequest request) {

        return Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .documentType(request.getDocumentType())
                .build();
    }


    public DocumentVersion toDocumentVersion(
            Document document,
            FileEntity fileEntity,
            int versionNumber,
            String changeNote
    ) {

        return DocumentVersion.builder()
                .document(document)
                .file(fileEntity)
                .versionNumber(versionNumber)
                .changeNote(changeNote)
                .build();
    }


    public DocumentUploadResponse toUploadResponse(
            Document document,
            FileEntity fileEntity,
            int totalVersions
    ) {

        return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .filename(fileEntity.getOriginalFilename())
                .currentVersionId(document.getCurrentVersion().getId())
                .versionNumber(document.getCurrentVersion().getVersionNumber())
                .totalVersions(totalVersions)
                .build();
    }
}
