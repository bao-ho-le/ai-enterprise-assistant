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
 * Unit tests for {@link DocumentChunkingStrategy}: page grouping, heading breadcrumbs, chunk index
 * continuity and sliding-window handling of oversized paragraphs.
 */
class DocumentChunkingStrategyTest {

    private final DocumentChunkingStrategy strategy =
            new DocumentChunkingStrategy(new ChunkingMapper(), new SlidingWindowSplitter());

    @Test
    void shouldReturnNoChunksWhenThereAreNoElements() {

        assertThat(strategy.chunk(extracted(List.of()))).isEmpty();
    }

    @Test
    void shouldGroupParagraphsByPageAndKeepOrder() {

        ExtractedText text = extracted(List.of(
                paragraph("first", 1),
                paragraph("second", 1),
                paragraph("third", 2)
        ));

        List<TextChunk> chunks = strategy.chunk(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getPageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("first\nsecond");
        assertThat(chunks.get(1).getPageNumber()).isEqualTo(2);
        assertThat(chunks.get(1).getContent()).isEqualTo("third");
        assertThat(chunks).extracting(TextChunk::getChunkIndex).containsExactly(0, 1);
    }

    @Test
    void shouldPrefixParagraphWithHeadingBreadcrumb() {

        ExtractedText text = extracted(List.of(
                heading("Chapter", 1, 1),
                paragraph("body", 1)
        ));

        assertThat(strategy.chunk(text).get(0).getContent()).isEqualTo("Chapter\nbody");
    }

    @Test
    void shouldNestHeadingsIntoPathAndDropSiblings() {

        String firstBody = "a".repeat(SlidingWindowSplitter.CHUNK_SIZE + 50);
        String secondBody = "b".repeat(SlidingWindowSplitter.CHUNK_SIZE + 50);

        ExtractedText text = extracted(List.of(
                heading("A", 1, 1),
                heading("B", 2, 1),
                paragraph(firstBody, 1),
                heading("C", 2, 1),
                paragraph(secondBody, 1)
        ));

        List<TextChunk> chunks = strategy.chunk(text);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        assertThat(chunks.get(0).getContent()).startsWith("A > B\n");
        assertThat(chunks.get(chunks.size() - 1).getContent()).startsWith("A > C\n");
        assertThat(chunks).noneSatisfy(chunk ->
                assertThat(chunk.getContent()).startsWith("A > B > C"));
    }

    @Test
    void shouldClearDeeperHeadingLevelsWhenParentChanges() {

        ExtractedText text = extracted(List.of(
                heading("A", 1, 1),
                heading("B", 2, 1),
                heading("New", 1, 1),
                paragraph("body", 1)
        ));

        assertThat(strategy.chunk(text).get(0).getContent()).isEqualTo("New\nbody");
    }

    @Test
    void shouldLimitBreadcrumbDepth() {

        ExtractedText text = extracted(List.of(
                heading("L1", 1, 1),
                heading("L2", 2, 1),
                heading("L3", 3, 1),
                heading("L4", 4, 1),
                paragraph("deep", 1)
        ));

        assertThat(strategy.chunk(text).get(0).getContent())
                .isEqualTo("L2 > L3 > L4\ndeep");
    }

    @Test
    void shouldSplitOversizedParagraphAndKeepBreadcrumbOnEveryPiece() {

        String longParagraph = "y".repeat(SlidingWindowSplitter.CHUNK_SIZE * 2);

        ExtractedText text = extracted(List.of(
                heading("Chapter", 1, 1),
                paragraph(longParagraph, 1)
        ));

        List<TextChunk> chunks = strategy.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getContent()).startsWith("Chapter\n"));
    }

    @Test
    void shouldFlushBufferWhenAddingParagraphWouldExceedChunkSize() {

        String first = "a".repeat(600);
        String second = "b".repeat(600);

        ExtractedText text = extracted(List.of(
                paragraph(first, 1),
                paragraph(second, 1)
        ));

        List<TextChunk> chunks = strategy.chunk(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo(first);
        assertThat(chunks.get(1).getContent()).isEqualTo(second);
    }

    @Test
    void shouldKeepChunkIndexContinuousAcrossPages() {

        ExtractedText text = extracted(List.of(
                paragraph("p1", 1),
                paragraph("p2", 2),
                paragraph("p3", 3)
        ));

        assertThat(strategy.chunk(text))
                .extracting(TextChunk::getChunkIndex)
                .containsExactly(0, 1, 2);
    }

    @Test
    void shouldTrackCharacterOffsetsWithinPage() {

        ExtractedText text = extracted(List.of(
                paragraph("abc", 1),
                paragraph("def", 1)
        ));

        TextChunk chunk = strategy.chunk(text).get(0);

        assertThat(chunk.getStartChar()).isZero();
        assertThat(chunk.getEndChar()).isPositive();
    }

    // ===================== helpers =====================

    private static ExtractedText extracted(List<DocumentElement> elements) {

        return ExtractedText.builder()
                .content("content")
                .elements(elements)
                .build();
    }

    private static DocumentElement paragraph(String content, int page) {

        return DocumentElement.builder()
                .type(ElementType.PARAGRAPH)
                .content(content)
                .pageNumber(page)
                .build();
    }

    private static DocumentElement heading(String content, int level, int page) {

        return DocumentElement.builder()
                .type(ElementType.HEADING)
                .content(content)
                .headingLevel(level)
                .pageNumber(page)
                .build();
    }
}
