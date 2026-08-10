package com.enterprise.aiassistant.backend.processing.extraction.service;

import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import org.springframework.core.io.Resource;

public interface TextExtractionService {

    ExtractedText extract(Resource resource, String mimeType);
}
