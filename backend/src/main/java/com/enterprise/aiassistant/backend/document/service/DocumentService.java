package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentUploadResponse upload(
            MultipartFile file,
            DocumentUploadRequest request
    );
}
