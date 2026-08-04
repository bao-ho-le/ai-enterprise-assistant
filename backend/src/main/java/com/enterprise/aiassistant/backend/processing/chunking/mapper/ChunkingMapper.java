package com.enterprise.aiassistant.backend.processing.chunking.mapper;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import org.springframework.stereotype.Component;

@Component
public class ChunkingMapper {

    // Ước lượng token: ~4 ký tự/token (quy ước phổ biến cho MVP, xem comment ở DocumentChunk.tokenCount).
    private static final int CHARS_PER_TOKEN = 4;

    public TextChunk toTextChunk(
            int chunkIndex,
            String content,
            int pageNumber,
            int startChar,
            int endChar
    ) {

        return TextChunk.builder()
                .chunkIndex(chunkIndex)
                .content(content)
                .pageNumber(pageNumber)
                .startChar(startChar)
                .endChar(endChar)
                .tokenCount(content.length() / CHARS_PER_TOKEN)
                .build();
    }
}
