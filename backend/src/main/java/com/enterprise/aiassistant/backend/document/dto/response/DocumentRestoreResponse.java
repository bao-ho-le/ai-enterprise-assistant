package com.enterprise.aiassistant.backend.document.dto.response;

import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentRestoreResponse {

    private Long documentId;

    private String title;

    private DocumentType documentType;

    private DocumentStatus status;

    private LocalDateTime deletedAt;

}
