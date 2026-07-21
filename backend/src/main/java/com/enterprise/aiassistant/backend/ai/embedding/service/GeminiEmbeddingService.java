package com.enterprise.aiassistant.backend.ai.embedding.service;

import com.enterprise.aiassistant.backend.ai.embedding.config.GeminiProperties;
import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingService implements EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final GeminiProperties properties;

    @Override
    public EmbeddingResult embed(String text) {

        validateText(text);

        try {
            Response<Embedding> response = embeddingModel.embed(text);
            Embedding embedding = response.content();

            return toEmbeddingResult(embedding, properties);

        } catch (Exception ex) {
            log.error(ErrorCode.EMBEDDING_FAILED.getMessage(), ex);
            throw new EmbeddingException(ErrorCode.EMBEDDING_FAILED, ex);
        }
    }


    // Helper

    private void validateText(String text){
        if (text == null || text.isBlank()) {
            throw new EmbeddingException(ErrorCode.EMBEDDING_TEXT_REQUIRED);
        }
    }

    // Mapper

    public EmbeddingResult toEmbeddingResult(Embedding embedding, GeminiProperties properties) {
        return EmbeddingResult.builder()
                .vector(embedding.vector())
                .dimension(embedding.dimension())
                .model(properties.getEmbeddingModel())
                .build();
    }

}
