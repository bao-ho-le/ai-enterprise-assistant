package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.VectorStoreException;
import dev.langchain4j.store.embedding.RelevanceScore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;


@Component
@RequiredArgsConstructor
public class VectorStoreHelper {

    private final QdrantProperties properties;

    // Ngưỡng cosine trong config (qdrant.score-threshold) đổi sang thang relevance (cosine + 1) / 2
    // mà EmbeddingSearchRequest.minScore dùng.
    public double minScore() {
        return RelevanceScore.fromCosineSimilarity(properties.getScoreThreshold());
    }

    // Mọi thao tác với EmbeddingStore đi qua đây để lỗi thành VectorStoreException:
    // DocumentProcessingRetryExecutor chỉ retry đúng loại lỗi này.
    public <T> T call(ErrorCode errorCode, Supplier<T> storeCall) {
        try {
            return storeCall.get();
        } catch (Exception e) {
            throw new VectorStoreException(errorCode, e);
        }
    }

}
