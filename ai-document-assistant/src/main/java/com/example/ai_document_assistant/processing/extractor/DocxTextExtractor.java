package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class DocxTextExtractor implements TextExtractor {

    private static final String DOCX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public boolean supports(String mimeType) {
        return DOCX_MIME_TYPE.equals(mimeType);
    }

    @Override
    public ExtractedText extract(byte[] fileBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String content = extractor.getText();
            return new ExtractedText(content, "APACHE_POI_DOCX", null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract text from DOCX", e);
        }
    }
}
