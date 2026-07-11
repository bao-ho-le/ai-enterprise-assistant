package com.enterprise.aiassistant.backend.document.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadNewVersionResponse {

    private Long documentId;

    private Long currentVersionId;

    private String filename;

    private int versionNumber;

    private String changeNote;

    private int totalVersions;

    private String status;
}
