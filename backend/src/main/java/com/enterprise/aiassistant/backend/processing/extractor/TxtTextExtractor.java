package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class TxtTextExtractor implements TextExtractor {

    private final ProcessingMapper processingMapper;

    @Override
    public boolean supports(String mimeType) {
        return "text/plain".equalsIgnoreCase(mimeType);
    }

    @Override
    public ExtractedText extract(Resource resource) {

        try {

            String content = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            return processingMapper.toExtractedText(
                    content,
                    ExtractionMethod.DIRECT_TEXT
            );

        } catch (IOException e) {

            throw new ProcessingException(
                    ErrorCode.TEXT_EXTRACTION_FAILED,
                    ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                    e
            );
        }
    }
}