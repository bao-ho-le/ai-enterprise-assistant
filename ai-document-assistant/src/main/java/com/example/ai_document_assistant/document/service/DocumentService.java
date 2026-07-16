package com.example.ai_document_assistant.document.service;

import com.example.ai_document_assistant.common.exception.ResourceNotFoundException;
import com.example.ai_document_assistant.document.dto.DocumentDetailResponse;
import com.example.ai_document_assistant.document.dto.DocumentResponse;
import com.example.ai_document_assistant.document.entity.Document;
import com.example.ai_document_assistant.document.entity.DocumentVersion;
import com.example.ai_document_assistant.document.enums.DocumentStatus;
import com.example.ai_document_assistant.document.repository.DocumentRepository;
import com.example.ai_document_assistant.document.repository.DocumentVersionRepository;
import com.example.ai_document_assistant.storage.entity.FileEntity;
import com.example.ai_document_assistant.storage.repository.FileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final FileRepository fileRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            FileRepository fileRepository
    ) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.fileRepository = fileRepository;
    }

    public Page<DocumentResponse> getAllDocuments(Pageable pageable) {
        return documentRepository.findAllByStatus(DocumentStatus.ACTIVE, pageable);
    }

    public DocumentDetailResponse getDocumentDetail(Long id) {
        Document document = documentRepository.findById(id)
                .filter(d -> d.getStatus() == DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        DocumentDetailResponse.DocumentInfo documentInfo = new DocumentDetailResponse.DocumentInfo(
                document.getTitle(),
                document.getDescription(),
                document.getStatus(),
                document.getDocumentType(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );

        DocumentVersion currentDocVersion = documentVersionRepository.findById(document.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current version not found for document id: " + id));

        FileEntity currentFile = fileRepository.findById(currentDocVersion.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found for version id: " + currentDocVersion.getId()));

        DocumentDetailResponse.CurrentVersionInfo currentVersionInfo = new DocumentDetailResponse.CurrentVersionInfo(
                currentDocVersion.getVersionNumber(),
                currentDocVersion.getStatus(),
                currentFile.getOriginalFilename(),
                currentFile.getExtension(),
                currentFile.getFileSize(),
                currentDocVersion.getCreatedAt()
        );

        List<DocumentVersion> allVersions = documentVersionRepository
                .findByDocumentIdOrderByVersionNumberDesc(id);

        List<DocumentDetailResponse.VersionHistoryItem> versionHistory = allVersions.stream()
                .map(v -> {
                    FileEntity f = fileRepository.findById(v.getFileId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "File not found for version id: " + v.getId()));
                    return new DocumentDetailResponse.VersionHistoryItem(
                            v.getVersionNumber(),
                            f.getOriginalFilename(),
                            v.getChangeNote(),
                            v.getStatus(),
                            v.getCreatedAt()
                    );
                })
                .toList();

        DocumentDetailResponse.AdvancedInfo advancedInfo = new DocumentDetailResponse.AdvancedInfo(
                currentFile.getStorageProvider(),
                currentFile.getBucketName(),
                currentFile.getObjectKey(),
                currentFile.getChecksum()
        );

        return new DocumentDetailResponse(documentInfo, currentVersionInfo, versionHistory, advancedInfo);
    }
}
