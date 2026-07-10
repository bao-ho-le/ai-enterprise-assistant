package com.enterprise.aiassistant.backend.document.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUploadResponse {


    private Long documentId;

    private Long currentVersionId;

    private String filename;

    private int versionNumber;

    private int totalVersions;

}