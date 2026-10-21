package com.enterprise.aiassistant.backend.processing.chunking.service;

import dev.langchain4j.data.segment.TextSegment;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;

import java.util.List;

public interface ChunkingService {

    List<TextSegment> chunk(ExtractedText extractedText, String fileExtension);
}
