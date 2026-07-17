package com.enterprise.aiassistant.backend.document.dto.response;

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

    private DocumentType documentType;

    private Integer currentVersion;

    private Long size;

    private VersionStatus status;
}

