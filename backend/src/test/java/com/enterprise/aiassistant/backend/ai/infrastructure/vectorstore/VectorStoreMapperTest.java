package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.processing.chunking.mapper.ChunkingMapper;
import com.enterprise.aiassistant.backend.processing.orchestration.mapper.ProcessingMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.RelevanceScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Một TextSegment đi suốt chunking -> lưu DB -> ghi Qdrant -> đọc kết quả search: tên key metadata
// ở các bước phải khớp nhau, id point cố định theo chunkId, và điểm trả ra vẫn là cosine như cũ.
class VectorStoreMapperTest {

    private final ChunkingMapper chunkingMapper = new ChunkingMapper();
    private final ProcessingMapper processingMapper = new ProcessingMapper();
    private final VectorStoreMapper vectorStoreMapper = new VectorStoreMapper();

    @Test
    void segmentCarriesChunkPositionAndIdentityThroughThePipeline() {

        TextSegment segment = chunkingMapper.toTextSegment(3, "Nội dung chunk dài hơn bốn ký tự", 2, 10, 42);

        DocumentVersion version = DocumentVersion.builder().id(7L).document(Document.builder().id(5L).build()).build();
        DocumentChunk chunk = processingMapper.toDocumentChunk(version, segment);
        assertEquals(3, chunk.getChunkIndex());
        assertEquals(2, chunk.getPageNumber());
        assertEquals(10, chunk.getStartChar());
        assertEquals(42, chunk.getEndChar());
        assertEquals(segment.text().length() / 4, chunk.getTokenCount());
        assertEquals(segment.text(), chunk.getContent());

        chunk.setId(42L);
        vectorStoreMapper.addChunkIdentity(segment, chunk, "gemini-embedding-001");
        assertEquals("00000000-0000-0000-0000-00000000002a", vectorStoreMapper.pointId(segment));

        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(RelevanceScore.fromCosineSimilarity(0.7), "id", null, segment);
        assertEquals(42L, vectorStoreMapper.chunkId(match));
        assertEquals(5L, vectorStoreMapper.documentId(match));
        assertEquals(7L, vectorStoreMapper.documentVersionId(match));
        assertEquals(2, vectorStoreMapper.pageNumber(match));
        assertEquals(10, vectorStoreMapper.startChar(match));
        assertEquals(42, vectorStoreMapper.endChar(match));
        assertEquals(0.7, vectorStoreMapper.cosineScore(match), 1e-12);
    }

    @Test
    void minScoreIsTheConfiguredCosineThresholdOnTheRelevanceScale() {

        QdrantProperties properties = new QdrantProperties();
        properties.setScoreThreshold(0.65f);

        double minScore = new VectorStoreHelper(properties).minScore();

        assertEquals(0.825, minScore, 1e-6);
        assertEquals(0.65f, (float) CosineSimilarity.fromRelevanceScore(minScore));
    }
}
