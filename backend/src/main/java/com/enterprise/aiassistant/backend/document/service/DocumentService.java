package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentUploadResponse upload(
            MultipartFile file,
            DocumentUploadRequest request
    );

    UploadNewVersionResponse uploadNewVersion(
            Long documentId,
            MultipartFile file,
            UploadNewVersionRequest request
    );


    DocumentDownloadResource downloadSelectedVersion(
            Long documentId,
            Long versionId
    );

    Resource loadProcessingResource(
            Long versionId
    );
    void deleteDocument(Long documentId);


    DocumentUpdateMetadataResponse updateDocumentMetadata(Long documentId, DocumentUpdateMetadataRequest request);

    boolean existsByTitle(String title);

    Page<DocumentListResponse> getDocuments(DocumentFilterRequest filter, Pageable pageable);

    DocumentDetailResponse getDocumentDetail(Long documentId);
}
