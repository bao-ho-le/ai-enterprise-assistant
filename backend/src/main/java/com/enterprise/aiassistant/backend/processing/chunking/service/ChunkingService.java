package com.enterprise.aiassistant.backend.processing.chunking.service;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;

import java.util.List;

public interface ChunkingService {

    List<TextChunk> chunk(ExtractedText extractedText, String fileExtension);
}
