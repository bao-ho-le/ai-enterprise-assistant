package com.enterprise.aiassistant.backend.processing.chunking.mapper;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChunkingMapper}.
 */
class ChunkingMapperTest {

    private final ChunkingMapper mapper = new ChunkingMapper();

    @Test
    void shouldMapAllChunkFieldsAndEstimateTokens() {

        TextChunk chunk = mapper.toTextChunk(3, "abcdefgh", 2, 10, 18);

        assertThat(chunk.getChunkIndex()).isEqualTo(3);
        assertThat(chunk.getContent()).isEqualTo("abcdefgh");
        assertThat(chunk.getPageNumber()).isEqualTo(2);
        assertThat(chunk.getStartChar()).isEqualTo(10);
        assertThat(chunk.getEndChar()).isEqualTo(18);
        assertThat(chunk.getTokenCount()).isEqualTo(2);
    }

    @Test
    void shouldEstimateZeroTokensForShortContent() {

        assertThat(mapper.toTextChunk(0, "abc", 1, 0, 3).getTokenCount()).isZero();
    }

    @Test
    void shouldEstimateOneTokenForFourCharacters() {

        assertThat(mapper.toTextChunk(0, "abcd", 1, 0, 4).getTokenCount()).isEqualTo(1);
    }
}
