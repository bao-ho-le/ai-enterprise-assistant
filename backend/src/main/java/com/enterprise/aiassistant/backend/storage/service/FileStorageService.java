package com.enterprise.aiassistant.backend.storage.service;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFileDto store(MultipartFile file);

    Resource loadAsResource(String bucketName, String objectKey);
}
