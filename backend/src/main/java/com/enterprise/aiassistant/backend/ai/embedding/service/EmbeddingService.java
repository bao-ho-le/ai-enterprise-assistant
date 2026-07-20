package com.enterprise.aiassistant.backend.ai.embedding.service;

import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;

public interface EmbeddingService {

    EmbeddingResult embed(String text);

}
