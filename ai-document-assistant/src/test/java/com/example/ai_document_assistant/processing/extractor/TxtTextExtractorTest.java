package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TxtTextExtractorTest {

    private final TxtTextExtractor extractor = new TxtTextExtractor();

    @Test
    void supports_shouldReturnTrue_forPlainTextMimeType() {
        assertTrue(extractor.supports("text/plain"));
    }

    @Test
    void supports_shouldReturnFalse_forOtherMimeType() {
        assertTrue(!extractor.supports("application/pdf"));
    }

    @Test
    void extract_shouldReturnCorrectContent() {
        String original = "Xin chào, đây là nội dung tiếng Việt.";
        byte[] fileBytes = original.getBytes(StandardCharsets.UTF_8);

        ExtractedText result = extractor.extract(fileBytes);

        assertEquals(original, result.content());
        assertEquals("PLAIN_TEXT", result.extractionMethod());
    }
}
