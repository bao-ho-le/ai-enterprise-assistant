package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class QdrantConfig {

    private final QdrantProperties properties;

    @Bean
    public QdrantClient qdrantClient() {

        QdrantGrpcClient.Builder grpcClient = QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getGrpcPort(),
                properties.getUseTls()
        );

        // Qdrant Cloud bắt buộc API key; Qdrant local để trống
        if (StringUtils.hasText(properties.getApiKey())) {
            grpcClient.withApiKey(properties.getApiKey());
        }

        return new QdrantClient(grpcClient.build());
    }

    // destroyMethod rỗng: QdrantClient đã được Spring đóng ở bean của chính nó.
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(QdrantClient qdrantClient) {

        return QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                .collectionName(properties.getCollectionName())
                .payloadTextKey(VectorStoreMapper.CONTENT)
                .build();
    }

}
