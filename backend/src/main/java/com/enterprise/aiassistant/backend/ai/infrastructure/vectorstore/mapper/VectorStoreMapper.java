package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper;

import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Metadata của TextSegment cũng chính là payload của point trên Qdrant (text nằm ở key CONTENT).
// Chunking ghi phần vị trí (chunkIndex, pageNumber...), addChunkIdentity gắn thêm id sau khi chunk
// được lưu DB. Giữ nguyên tên key của payload cũ.
@Component
public class VectorStoreMapper {

    public static final String CONTENT = "content";
    public static final String CHUNK_ID = "chunkId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String DOCUMENT_VERSION_ID = "documentVersionId";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String START_CHAR = "startChar";
    public static final String END_CHAR = "endChar";
    public static final String TOKEN_COUNT = "tokenCount";
    public static final String EMBEDDING_MODEL = "embeddingModel";

    public void addChunkIdentity(TextSegment segment, DocumentChunk chunk, String embeddingModel) {
        segment.metadata()
                .put(CHUNK_ID, chunk.getId())
                .put(DOCUMENT_ID, chunk.getDocumentVersion().getDocument().getId())
                .put(DOCUMENT_VERSION_ID, chunk.getDocumentVersion().getId())
                .put(EMBEDDING_MODEL, embeddingModel);
    }

    // QdrantEmbeddingStore chỉ nhận id dạng UUID; sinh cố định từ chunkId để retry ghi đè đúng point cũ.
    public String pointId(TextSegment segment) {
        return new UUID(0L, segment.metadata().getLong(CHUNK_ID)).toString();
    }

    public Long chunkId(EmbeddingMatch<TextSegment> match) {
        return match.embedded().metadata().getLong(CHUNK_ID);
    }

    public Long documentId(EmbeddingMatch<TextSegment> match) {
        return match.embedded().metadata().getLong(DOCUMENT_ID);
    }

    public Long documentVersionId(EmbeddingMatch<TextSegment> match) {
        return match.embedded().metadata().getLong(DOCUMENT_VERSION_ID);
    }

    public Integer pageNumber(EmbeddingMatch<TextSegment> match) {
        return match.embedded().metadata().getInteger(PAGE_NUMBER);
    }

    public Integer startChar(EmbeddingMatch<TextSegment> match) {
        return match.embedded().metadata().getInteger(START_CHAR);
    }

    public Integer endChar(EmbeddingMatch<TextSegment> match) {
        return match.embedded().metadata().getInteger(END_CHAR);
    }

    // EmbeddingStore trả relevance score = (cosine + 1) / 2; đổi về cosine để điểm lưu DB và
    // "% match" trên giao diện giữ như cũ.
    public double cosineScore(EmbeddingMatch<TextSegment> match) {
        return CosineSimilarity.fromRelevanceScore(match.score());
    }
}
