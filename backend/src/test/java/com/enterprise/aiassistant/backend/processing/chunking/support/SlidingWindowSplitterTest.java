package com.enterprise.aiassistant.backend.processing.chunking.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlidingWindowSplitter}: window boundaries, overlap and edge cases.
 */
class SlidingWindowSplitterTest {

    private final SlidingWindowSplitter splitter = new SlidingWindowSplitter();

    @Test
    void shouldReturnEmptyListForNullAndEmptyText() {

        assertThat(splitter.split(null)).isEmpty();
        assertThat(splitter.split("")).isEmpty();
    }

    @Test
    void shouldReturnShortTextAsSinglePiece() {

        assertThat(splitter.split("hello")).containsExactly("hello");
    }

    @Test
    void shouldReturnExactlyChunkSizeAsSinglePiece() {

        String text = "a".repeat(SlidingWindowSplitter.CHUNK_SIZE);

        assertThat(splitter.split(text)).containsExactly(text);
    }

    @Test
    void shouldSplitTextOverChunkSizeWithOverlap() {

        String text = "a".repeat(SlidingWindowSplitter.CHUNK_SIZE + SlidingWindowSplitter.OVERLAP_SIZE);

        List<String> pieces = splitter.split(text);

        assertThat(pieces).hasSize(2);
        assertThat(pieces.get(0)).hasSize(SlidingWindowSplitter.CHUNK_SIZE);
        assertThat(pieces.get(1)).hasSize(SlidingWindowSplitter.OVERLAP_SIZE * 2);
        assertThat(pieces.get(1)).startsWith(
                pieces.get(0).substring(SlidingWindowSplitter.CHUNK_SIZE - SlidingWindowSplitter.OVERLAP_SIZE));
    }

    @Test
    void shouldKeepOverlapBetweenConsecutivePieces() {

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < 3000; i++) {
            text.append((char) ('a' + (i % 26)));
        }

        List<String> pieces = splitter.split(text.toString());

        assertThat(pieces).hasSizeGreaterThan(1);

        for (int i = 1; i < pieces.size(); i++) {
            String previousTail = pieces.get(i - 1)
                    .substring(pieces.get(i - 1).length() - SlidingWindowSplitter.OVERLAP_SIZE);
            String currentHead = pieces.get(i).substring(0, SlidingWindowSplitter.OVERLAP_SIZE);

            assertThat(currentHead).isEqualTo(previousTail);
        }
    }

    @Test
    void shouldNotLoseCharacters() {

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < 2500; i++) {
            text.append((char) ('0' + (i % 10)));
        }

        String joined = String.join("", splitter.split(text.toString()));

        assertThat(joined).startsWith(text.substring(0, SlidingWindowSplitter.CHUNK_SIZE));
        assertThat(joined).endsWith(text.substring(text.length() - SlidingWindowSplitter.CHUNK_SIZE));
    }

    @Test
    void shouldProduceMoreThanTwoPiecesForVeryLongText() {

        String text = "x".repeat(10_000);

        List<String> pieces = splitter.split(text);

        assertThat(pieces).hasSizeGreaterThanOrEqualTo(12);
        assertThat(pieces).allSatisfy(piece ->
                assertThat(piece.length()).isLessThanOrEqualTo(SlidingWindowSplitter.CHUNK_SIZE));
    }
}
