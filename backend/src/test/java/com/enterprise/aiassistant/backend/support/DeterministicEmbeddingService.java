package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;

/**
 * Deterministic, offline {@link EmbeddingService} used by integration tests.
 *
 * <p>Same text always produces the same vector, so vector similarity assertions are stable.
 * No network access, no API key, no quota.
 */
public class DeterministicEmbeddingService implements EmbeddingService {

    public static final String MODEL_NAME = "deterministic-test-embedding";

    private static final int CHARS_PER_TOKEN = 4;

    private final int dimension;

    public DeterministicEmbeddingService(int dimension) {

        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }

        this.dimension = dimension;
    }

    @Override
    public EmbeddingResult embed(String text) {

        if (text == null || text.isBlank()) {
            throw new EmbeddingException(ErrorCode.EMBEDDING_TEXT_REQUIRED);
        }

        float[] vector = new float[dimension];

        int seed = seedOf(text);

        for (int i = 0; i < dimension; i++) {
            // floorMod keeps every component in [0.01, 1.01] and is never negative.
            vector[i] = 0.01f + (Math.floorMod(seed + (long) i * 2_654_435_761L, 1000) / 1000f);
        }

        return EmbeddingResult.builder()
                .vector(vector)
                .dimension(dimension)
                .model(MODEL_NAME)
                .inputTokens(Math.max(1, text.length() / CHARS_PER_TOKEN))
                .build();
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * Vector for the given text, computed the same way as {@link #embed(String)}
     * (so tests can seed Qdrant and query with the same content).
     */
    public float[] vectorOf(String text) {
        return embed(text).getVector();
    }

    private int seedOf(String text) {

        int seed = 7;

        for (int i = 0; i < text.length(); i++) {
            seed = 31 * seed + text.charAt(i);
        }

        return seed;
    }
}
