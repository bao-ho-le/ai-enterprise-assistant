package com.enterprise.aiassistant.backend.processing.extraction.extractor;

import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import org.springframework.core.io.Resource;


public interface TextExtractor {

    boolean supports(String mimeType);

    ExtractedText extract(Resource resource);
}
