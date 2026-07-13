package com.enterprise.aiassistant.backend.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "file.upload")
@Getter
@Setter
public class FileUploadProperties {

    private DataSize maxSize;
}
