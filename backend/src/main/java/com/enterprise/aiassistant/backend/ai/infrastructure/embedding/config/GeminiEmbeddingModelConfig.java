package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.config;

import com.enterprise.aiassistant.backend.ai.analytics.usage.listener.AIUsageListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class GeminiEmbeddingModelConfig {

    private final GeminiProperties properties;

    // Exposed as the provider-agnostic LangChain4j EmbeddingModel type so callers
    // only depend on the SDK abstraction, not this Gemini class.
    // Mọi lượt embedding (xử lý tài liệu, semantic search, Document QA) đều ghi AI usage log.
    @Bean
    public EmbeddingModel embeddingModel(AIUsageListener aiUsageListener) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getEmbeddingModel())
                .listeners(List.of(aiUsageListener))
                .build();
    }

}
