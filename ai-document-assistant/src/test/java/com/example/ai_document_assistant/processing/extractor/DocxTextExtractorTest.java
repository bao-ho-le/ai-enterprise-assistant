package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxTextExtractorTest {

    private final DocxTextExtractor extractor = new DocxTextExtractor();

    @Test
    void supports_shouldReturnTrue_forDocxMimeType() {
        assertTrue(extractor.supports(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void extract_shouldReturnCorrectContent() throws IOException {
        byte[] fileBytes = createSampleDocx("Hello from DOCX test");

        ExtractedText result = extractor.extract(fileBytes);

        assertTrue(result.content().contains("Hello from DOCX test"));
        assertEquals("APACHE_POI_DOCX", result.extractionMethod());
    }

    private byte[] createSampleDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
