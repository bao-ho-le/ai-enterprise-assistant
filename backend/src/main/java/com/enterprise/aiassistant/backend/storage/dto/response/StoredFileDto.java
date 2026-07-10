package com.enterprise.aiassistant.backend.storage.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoredFileDto {

    private String originalName;

    private String storedName;

    private String bucket;

    private String objectKey;

    private String contentType;

    private Long size;
}
