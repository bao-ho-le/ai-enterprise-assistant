package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.helper;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.config.GeminiProperties;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingHelper {

    // Ước lượng token: ~4 ký tự/token (cùng quy ước với ChunkingMapper.CHARS_PER_TOKEN).
    private static final int CHARS_PER_TOKEN = 4;

    private final GeminiProperties properties;

    // Tên model ghi vào AI usage log và payload Qdrant, kể cả khi lời gọi embedding thất bại.
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

    // GoogleAiEmbeddingModel (langchain4j) không gắn TokenUsage cho embedding, nên ước lượng theo số ký tự
    // của từng đoạn rồi cộng lại, để usage log vẫn có số token khác 0.
    public Integer inputTokens(Response<?> response, List<String> texts) {
        if (response.tokenUsage() != null) {
            return response.tokenUsage().inputTokenCount();
        }
        return texts.stream()
                .mapToInt(text -> Math.max(1, text.length() / CHARS_PER_TOKEN))
                .sum();
    }
}
