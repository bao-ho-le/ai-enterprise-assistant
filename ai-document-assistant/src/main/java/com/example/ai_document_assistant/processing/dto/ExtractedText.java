package com.example.ai_document_assistant.processing.dto;

public record ExtractedText(
        String content,
        String extractionMethod,
        String language
) {
}
