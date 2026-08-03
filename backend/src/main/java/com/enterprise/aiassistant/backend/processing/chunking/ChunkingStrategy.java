package com.enterprise.aiassistant.backend.processing.chunking;

import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;

import java.util.List;

public interface ChunkingStrategy {

    List<TextChunk> chunk(ExtractedText extractedText);
}
