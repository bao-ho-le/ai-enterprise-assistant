package com.enterprise.aiassistant.backend.document.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Data
@Builder
@Getter
@Setter
public class DocumentDownloadResource {
    private Resource resource;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
}
