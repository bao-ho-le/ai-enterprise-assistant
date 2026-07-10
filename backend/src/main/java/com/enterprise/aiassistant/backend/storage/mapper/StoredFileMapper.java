package com.enterprise.aiassistant.backend.storage.mapper;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class StoredFileMapper {

    public StoredFileDto toDto(
            MultipartFile file,
            String storedName,
            String bucket
    ) {

        return StoredFileDto.builder()
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .bucket(bucket)
                .objectKey(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }
}