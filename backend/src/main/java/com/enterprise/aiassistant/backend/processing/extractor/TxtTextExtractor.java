package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extractor.tika.TikaContentExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class TxtTextExtractor implements TextExtractor {

    private final TikaContentExtractor tikaContentExtractor;

    @Override
    public boolean supports(String mimeType) {
        return "text/plain".equalsIgnoreCase(mimeType);
    }

    @Override
    public ExtractedText extract(Resource resource) {

        try (InputStream inputStream = resource.getInputStream()) {

            return tikaContentExtractor.extract(inputStream, resource.getFilename());

        } catch (IOException e) {

            throw new ProcessingException(
                    ErrorCode.TEXT_EXTRACTION_FAILED,
                    ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                    e
            );
        }
    }
}
