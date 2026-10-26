package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.helper;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.config.GeminiProperties;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingHelper {

    private final GeminiProperties properties;

    // Tên model ghi vào payload Qdrant (usage log lấy tên model ở AIUsageListener).
    public String getModelName() {
        return properties.getEmbeddingModel();
    }

    // Mọi lời gọi EmbeddingModel đi qua đây để lỗi thành EmbeddingException:
    // DocumentProcessingRetryExecutor chỉ retry đúng loại lỗi này.
    public <T> T call(Supplier<T> embeddingCall) {
        try {
            return embeddingCall.get();
        } catch (Exception ex) {
            log.error(ErrorCode.EMBEDDING_FAILED.getMessage(), ex);
            throw new EmbeddingException(ErrorCode.EMBEDDING_FAILED, ex);
        }
    }
}
