package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TxtTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String mimeType) {
        return "text/plain".equals(mimeType);
    }

    @Override
    public ExtractedText extract(byte[] fileBytes) {
        String content = new String(fileBytes, StandardCharsets.UTF_8);
        return new ExtractedText(content, "PLAIN_TEXT", null);
    }
}
