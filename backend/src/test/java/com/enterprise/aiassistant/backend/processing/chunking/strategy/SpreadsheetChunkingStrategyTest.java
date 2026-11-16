package com.enterprise.aiassistant.backend.processing.chunking.strategy;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.chunking.mapper.ChunkingMapper;
import com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpreadsheetChunkingStrategy}: header re-injection, sheet grouping and
 * oversized row handling.
 */
class SpreadsheetChunkingStrategyTest {

    private final SpreadsheetChunkingStrategy strategy =
            new SpreadsheetChunkingStrategy(new ChunkingMapper(), new SlidingWindowSplitter());

    @Test
    void shouldReturnNoChunksForEmptyElements() {

        assertThat(strategy.chunk(extracted(List.of()))).isEmpty();
    }

    @Test
    void shouldEmitSingleChunkForHeaderOnlySheet() {

        List<TextChunk> chunks = strategy.chunk(extracted(List.of(row("id,name", 1))));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("id,name");
        assertThat(chunks.get(0).getPageNumber()).isEqualTo(1);
    }

    @Test
    void shouldKeepHeaderInFirstChunk() {

        List<TextChunk> chunks = strategy.chunk(extracted(List.of(
                row("id,name", 1),
                row("1,Alice", 1),
                row("2,Bob", 1)
        )));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("id,name\n1,Alice\n2,Bob");
    }

    @Test
    void shouldReinjectHeaderWhenChunkIsFlushed() {

        String firstRow = "a".repeat(600);
        String secondRow = "b".repeat(600);

        List<TextChunk> chunks = strategy.chunk(extracted(List.of(
                row("header", 1),
                row(firstRow, 1),
                row(secondRow, 1)
        )));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("header\n" + firstRow);
        assertThat(chunks.get(1).getContent()).isEqualTo("header\n" + secondRow);
        assertThat(chunks).extracting(TextChunk::getChunkIndex).containsExactly(0, 1);
    }

    @Test
    void shouldSplitOversizedRowAndPrefixEachPieceWithHeader() {

        String longRow = "x".repeat(SlidingWindowSplitter.CHUNK_SIZE * 2);

        List<TextChunk> chunks = strategy.chunk(extracted(List.of(
                row("header", 1),
                row(longRow, 1)
        )));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getContent()).startsWith("header\n"));
    }

    @Test
    void shouldNotFlushHeaderOnlyBufferBeforeOversizedRow() {

        String longRow = "z".repeat(SlidingWindowSplitter.CHUNK_SIZE + 1);

        List<TextChunk> chunks = strategy.chunk(extracted(List.of(
                row("header", 1),
                row(longRow, 1)
        )));

        assertThat(chunks).noneSatisfy(chunk ->
                assertThat(chunk.getContent()).isEqualTo("header"));
    }

    @Test
    void shouldKeepChunkIndexContinuousAcrossSheets() {

        List<TextChunk> chunks = strategy.chunk(extracted(List.of(
                row("h1", 1),
                row("v1", 1),
                row("h2", 2),
                row("v2", 2)
        )));

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(TextChunk::getChunkIndex).containsExactly(0, 1);
        assertThat(chunks).extracting(TextChunk::getPageNumber).containsExactly(1, 2);
        assertThat(chunks.get(0).getContent()).isEqualTo("h1\nv1");
        assertThat(chunks.get(1).getContent()).isEqualTo("h2\nv2");
    }

    // ===================== helpers =====================

    private static ExtractedText extracted(List<DocumentElement> elements) {

        return ExtractedText.builder()
                .content("content")
                .elements(elements)
                .build();
    }

    private static DocumentElement row(String content, int sheet) {

        return DocumentElement.builder()
                .type(ElementType.TABLE_ROW)
                .content(content)
                .pageNumber(sheet)
                .build();
    }
}
