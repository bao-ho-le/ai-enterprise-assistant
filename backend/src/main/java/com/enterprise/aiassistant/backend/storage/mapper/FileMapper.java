package com.enterprise.aiassistant.backend.storage.mapper;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.provider}")
    private String provider;

    public FileEntity toFileEntity(StoredFileDto storedFile) {

        return FileEntity.builder()
                .originalFilename(storedFile.getOriginalName())
                .storedFilename(storedFile.getStoredName())
                .bucketName(bucket)
                .objectKey(storedFile.getObjectKey())
                .mimeType(storedFile.getContentType())
                .fileSize(storedFile.getSize())
                .extension(getExtension(storedFile.getOriginalName()))
                .storageProvider(provider)
                .build();
    }

    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return null;
        }

        return filename.substring(
                filename.lastIndexOf(".") + 1
        ).toLowerCase();
    }


}
