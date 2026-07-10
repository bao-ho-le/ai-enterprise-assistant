package com.enterprise.aiassistant.backend.document.dto.request;

import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadRequest {

    private Long documentId;

    private String title;

    private String description;

    private String changeNote;

    private DocumentType documentType;
}