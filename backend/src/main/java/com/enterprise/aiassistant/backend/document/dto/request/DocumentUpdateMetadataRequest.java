package com.enterprise.aiassistant.backend.document.dto.request;

import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import lombok.Data;

@Data
public class DocumentUpdateMetadataRequest {

    private String title;

    private String description;

    private DocumentType documentType;
}
