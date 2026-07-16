package com.enterprise.aiassistant.backend.document.dto.response;

import org.springframework.core.io.Resource;

public record DocumentDownloadResource(
        Resource resource,
        String originalFilename,
        String mimeType,
        Long fileSize
) {
}
