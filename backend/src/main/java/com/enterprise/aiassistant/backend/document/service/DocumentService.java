package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
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


    DocumentDownloadResource downloadCurrentVersion(
            Long documentId
    );

    void deleteDocument(Long documentId);


    DocumentUpdateMetadataResponse updateDocumentMetadata(Long documentId, DocumentUpdateMetadataRequest request);
}
