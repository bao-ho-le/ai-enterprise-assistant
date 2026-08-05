package com.enterprise.aiassistant.backend.document.dto.request;

import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import lombok.Data;

@Data
public class DocumentUploadRequest {

    private String title;

    private String description;

    private DocumentType documentType;

    // null = upload vào thư mục gốc (root), backend tự gán chứ không để trống
    private Long folderId;
}
