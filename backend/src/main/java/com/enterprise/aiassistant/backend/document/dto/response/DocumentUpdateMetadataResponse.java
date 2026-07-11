package com.enterprise.aiassistant.backend.document.dto.response;

import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUpdateMetadataResponse {

    private Long documentId;

    private String title;

    private String description;

    private DocumentType documentType;

}
