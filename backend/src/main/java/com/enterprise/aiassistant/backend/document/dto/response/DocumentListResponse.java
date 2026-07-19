package com.enterprise.aiassistant.backend.document.dto.response;

import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListResponse {

    private Long id;

    private String title;

    private LocalDateTime uploadTime;

    private String extension;

    private DocumentType documentType;

    private Long size;

    // DocumentVersion.status (processing pipeline state of the current version)
    private VersionStatus versionStatus;

    // Document.status (soft-delete state of the document itself)
    private DocumentStatus documentStatus;
}
