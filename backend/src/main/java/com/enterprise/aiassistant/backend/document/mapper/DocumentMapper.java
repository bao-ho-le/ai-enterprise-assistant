package com.enterprise.aiassistant.backend.document.mapper;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
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
            FileEntity fileEntity
    ) {

        return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .currentVersionId(document.getCurrentVersion().getId())
                .title(document.getTitle())
                .filename(fileEntity.getOriginalFilename())
                .versionNumber(document.getCurrentVersion().getVersionNumber())
                .status(document.getStatus())
                .build();
    }

    public DocumentUpdateMetadataResponse toUpdateMetadataReponse(Document document) {

        return DocumentUpdateMetadataResponse.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .documentType(document.getDocumentType())
                .build();
    }

    public UploadNewVersionResponse toUploadNewVersionResponse(
            Document document,
            DocumentVersion version,
            FileEntity fileEntity){

        return UploadNewVersionResponse.builder()
                .documentId(document.getId())
                .currentVersionId(version.getId())
                .filename(fileEntity.getOriginalFilename())
                .versionNumber(version.getVersionNumber())
                .changeNote(version.getChangeNote())
                .status(version.getStatus().name())
                .build();
    }
}
