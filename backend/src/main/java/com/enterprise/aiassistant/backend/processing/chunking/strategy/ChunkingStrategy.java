package com.enterprise.aiassistant.backend.processing.chunking.strategy;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;

import java.util.List;

public interface ChunkingStrategy {

    List<TextChunk> chunk(ExtractedText extractedText);
}
