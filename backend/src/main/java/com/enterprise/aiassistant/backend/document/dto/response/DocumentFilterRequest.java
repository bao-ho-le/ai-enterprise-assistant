package com.enterprise.aiassistant.backend.document.dto.response;

import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DocumentFilterRequest {

    private String keyword;

    /**
     * newest
     * oldest
     */
    private String sort;

    private DocumentType documentType;

    private String extension;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;

    private Long minSize;

    private Long maxSize;

    private VersionStatus status;

}
