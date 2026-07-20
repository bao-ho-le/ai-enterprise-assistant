package com.enterprise.aiassistant.backend.ai.embedding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey;

    private String embeddingModel;

}