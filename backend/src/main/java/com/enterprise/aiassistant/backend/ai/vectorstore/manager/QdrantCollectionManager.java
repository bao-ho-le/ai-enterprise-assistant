package com.enterprise.aiassistant.backend.ai.vectorstore.manager;

import com.enterprise.aiassistant.backend.ai.vectorstore.config.QdrantProperties;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantCollectionManager {

    private final QdrantClient qdrantClient;
    private final QdrantProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {

        boolean exists = Futures.getUnchecked(
                qdrantClient.collectionExistsAsync(properties.getCollectionName())
        );

        if (exists) {
            log.info("Collection [{}] already exists.", properties.getCollectionName());
            return;
        }

        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                .setSize(properties.getVectorSize())
                .setDistance(Collections.Distance.Cosine)
                .build();

        Futures.getUnchecked(
                qdrantClient.createCollectionAsync(properties.getCollectionName(), vectorParams)
        );

        log.info("Collection [{}] created.", properties.getCollectionName());
    }

}