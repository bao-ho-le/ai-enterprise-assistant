package com.enterprise.aiassistant.backend.processing.extraction.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.extractor.tika.TikaContentExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class DocxTextExtractor implements TextExtractor {

    private static final String DOCX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final TikaContentExtractor tikaContentExtractor;

    @Override
    public boolean supports(String mimeType) {
        return DOCX_MIME_TYPE.equalsIgnoreCase(mimeType);
    }

    @Override
    public ExtractedText extract(Resource resource) {

        try (InputStream inputStream = resource.getInputStream()) {

            // DOCX không có ranh giới trang thật trong model dữ liệu -> Tika không sinh div.page, pageNumber = 1.
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
