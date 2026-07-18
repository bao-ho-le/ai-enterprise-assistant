package com.enterprise.aiassistant.backend.processing.service;

import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extractor.TextExtractor;
import com.enterprise.aiassistant.backend.processing.extractor.TextExtractorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextExtractionService {

    private final TextExtractorFactory extractorFactory;


    public ExtractedText extract(
            Resource resource,
            String mimeType
    ){

        TextExtractor extractor =
                extractorFactory.getExtractor(mimeType);


        return extractor.extract(resource);
    }

}
