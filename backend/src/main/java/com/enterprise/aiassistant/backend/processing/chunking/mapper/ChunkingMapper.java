package com.enterprise.aiassistant.backend.processing.chunking.mapper;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import static com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper.*;

@Component
public class ChunkingMapper {

    // Ước lượng token: ~4 ký tự/token (quy ước phổ biến cho MVP, xem comment ở DocumentChunk.tokenCount).
    private static final int CHARS_PER_TOKEN = 4;

    // Vị trí của chunk đi theo metadata của TextSegment; sau khi lưu DB, cùng segment này được gắn
    // thêm id rồi ghi lên Qdrant (xem VectorStoreMapper).
    public TextSegment toTextSegment(
            int chunkIndex,
            String content,
            int pageNumber,
            int startChar,
            int endChar
    ) {

        return TextSegment.from(content, new Metadata()
                .put(CHUNK_INDEX, chunkIndex)
                .put(PAGE_NUMBER, pageNumber)
                .put(START_CHAR, startChar)
                .put(END_CHAR, endChar)
                .put(TOKEN_COUNT, content.length() / CHARS_PER_TOKEN));
    }
}
