package com.enterprise.aiassistant.backend.ai.vectorstore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qdrant")
@Getter
@Setter
public class QdrantProperties {

    private String host;

    private Integer grpcPort;

    private Integer restPort;

    private String collectionName;

    private Integer vectorSize;

    private Boolean useTls;

    private String apiKey;
}