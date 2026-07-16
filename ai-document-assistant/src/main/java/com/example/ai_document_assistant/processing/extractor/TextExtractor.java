package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;

public interface TextExtractor {

    boolean supports(String mimeType);

    ExtractedText extract(byte[] fileBytes);
}
