package com.enterprise.aiassistant.backend.processing.chunking;

import com.enterprise.aiassistant.backend.processing.chunking.document.DocumentChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.spreadsheet.SpreadsheetChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter;
import com.enterprise.aiassistant.backend.processing.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.enums.ElementType;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter.CHUNK_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingStrategyTest {

    private final SlidingWindowSplitter splitter = new SlidingWindowSplitter();

    private final ProcessingMapper mapper = new ProcessingMapper();

    private final DocumentChunkingStrategy documentStrategy =
            new DocumentChunkingStrategy(mapper, splitter);

    private final SpreadsheetChunkingStrategy spreadsheetStrategy =
            new SpreadsheetChunkingStrategy(mapper, splitter);

    @Test
    void documentStrategyKeepsHeadingContextAndContinuousChunkIndex() {

        List<TextChunk> chunks = documentStrategy.chunk(extractedText(List.of(
                element(ElementType.HEADING, "Chapter 1", 1),
                element(ElementType.PARAGRAPH, "a".repeat(600), 1),
                element(ElementType.PARAGRAPH, "b".repeat(600), 1),
                element(ElementType.HEADING, "Chapter 2", 2),
                element(ElementType.PARAGRAPH, "c".repeat(100), 2)
        )));

        // 2 paragraph 600 ký tự không gộp chung được -> 2 chunk ở trang 1, trang 2 thêm 1 chunk
        assertEquals(3, chunks.size());
        assertEquals(List.of(0, 1, 2), chunks.stream().map(TextChunk::getChunkIndex).toList());
        assertTrue(chunks.get(0).getContent().startsWith("Chapter 1\n"));
        assertTrue(chunks.get(1).getContent().startsWith("Chapter 1\n"));
        assertTrue(chunks.get(2).getContent().startsWith("Chapter 2\n"));
        assertEquals(List.of(1, 1, 2), chunks.stream().map(TextChunk::getPageNumber).toList());
    }

    @Test
    void documentStrategyFallsBackToSlidingWindowForOversizedParagraph() {

        List<TextChunk> chunks = documentStrategy.chunk(extractedText(List.of(
                element(ElementType.PARAGRAPH, "x".repeat(2500), 1)
        )));

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().length() <= CHUNK_SIZE));
        assertEquals(List.of(0, 1, 2), chunks.stream().map(TextChunk::getChunkIndex).toList());
    }

    @Test
    void spreadsheetStrategyRepeatsHeaderPerSheetAndContinuesChunkIndex() {

        List<TextChunk> chunks = spreadsheetStrategy.chunk(extractedText(List.of(
                element(ElementType.TABLE_ROW, "id\tname", 1),
                element(ElementType.TABLE_ROW, "1\t" + "r".repeat(600), 1),
                element(ElementType.TABLE_ROW, "2\t" + "s".repeat(600), 1),
                element(ElementType.TABLE_ROW, "code\tvalue", 2),
                element(ElementType.TABLE_ROW, "A\t10", 2)
        )));

        assertEquals(3, chunks.size());
        assertEquals(List.of(0, 1, 2), chunks.stream().map(TextChunk::getChunkIndex).toList());
        assertTrue(chunks.get(0).getContent().startsWith("id\tname"));
        assertTrue(chunks.get(1).getContent().startsWith("id\tname"));
        assertTrue(chunks.get(2).getContent().startsWith("code\tvalue"));
        assertEquals(List.of(1, 1, 2), chunks.stream().map(TextChunk::getPageNumber).toList());
    }

    @Test
    void spreadsheetStrategySplitsOversizedRowKeepingHeader() {

        List<TextChunk> chunks = spreadsheetStrategy.chunk(extractedText(List.of(
                element(ElementType.TABLE_ROW, "id\tnote", 1),
                element(ElementType.TABLE_ROW, "y".repeat(2500), 1)
        )));

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().startsWith("id\tnote")));
    }

    private ExtractedText extractedText(List<DocumentElement> elements) {
        return ExtractedText.builder().elements(elements).build();
    }

    private DocumentElement element(ElementType type, String content, int pageNumber) {
        return DocumentElement.builder()
                .type(type)
                .content(content)
                .pageNumber(pageNumber)
                .build();
    }
}
