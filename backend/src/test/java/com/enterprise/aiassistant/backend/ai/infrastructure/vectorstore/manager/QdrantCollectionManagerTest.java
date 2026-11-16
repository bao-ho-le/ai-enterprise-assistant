package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.manager;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QdrantCollectionManager}: collection creation is idempotent and uses the
 * configured vector size with cosine distance.
 */
class QdrantCollectionManagerTest {

    private static final String COLLECTION = "test_document_chunks";

    private final QdrantClient qdrantClient = mock(QdrantClient.class);

    private QdrantCollectionManager manager;

    @BeforeEach
    void setUp() {

        QdrantProperties properties = new QdrantProperties();
        properties.setCollectionName(COLLECTION);
        properties.setVectorSize(8);

        manager = new QdrantCollectionManager(qdrantClient, properties);
    }

    @Test
    void shouldCreateCollectionWithConfiguredDimensionsAndCosineDistance() {

        when(qdrantClient.collectionExistsAsync(COLLECTION)).thenReturn(Futures.immediateFuture(false));
        when(qdrantClient.createCollectionAsync(eq(COLLECTION), any(Collections.VectorParams.class)))
                .thenReturn(Futures.immediateFuture(Collections.CollectionOperationResponse.getDefaultInstance()));

        manager.initialize();

        ArgumentCaptor<Collections.VectorParams> captor =
                ArgumentCaptor.forClass(Collections.VectorParams.class);
        verify(qdrantClient).createCollectionAsync(eq(COLLECTION), captor.capture());

        assertThat(captor.getValue().getSize()).isEqualTo(8);
        assertThat(captor.getValue().getDistance()).isEqualTo(Collections.Distance.Cosine);
    }

    @Test
    void shouldSkipCreationWhenCollectionAlreadyExists() {

        when(qdrantClient.collectionExistsAsync(COLLECTION)).thenReturn(Futures.immediateFuture(true));

        manager.initialize();
        manager.initialize();

        verify(qdrantClient, never()).createCollectionAsync(anyString(), any(Collections.VectorParams.class));
    }
}
