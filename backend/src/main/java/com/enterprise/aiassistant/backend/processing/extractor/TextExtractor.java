package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;

import java.io.File;
import java.io.IOException;

public interface TextExtractor {

    boolean supports(String mimeType);

    ExtractedText extract(File file) throws IOException;
}