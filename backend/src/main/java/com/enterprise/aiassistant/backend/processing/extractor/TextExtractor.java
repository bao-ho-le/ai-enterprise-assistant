package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import jakarta.annotation.Resource;

import java.io.IOException;

public interface TextExtractor {

    boolean supports(String mimeType);

    ExtractedText extract(Resource resource);
}