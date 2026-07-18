package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class PdfTextExtractor implements TextExtractor {

    private final ProcessingMapper processingMapper;

    @Override
    public boolean supports(String mimeType) {
        return MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(mimeType);
    }


    @Override
    public ExtractedText extract(Resource resource) {

        try (
                InputStream inputStream = resource.getInputStream();
                PDDocument document = Loader.loadPDF(inputStream.readAllBytes())
        ) {

            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);

            return processingMapper.toExtractedText(content, ExtractionMethod.DIRECT_TEXT);

        } catch (IOException e) {

            throw new ProcessingException(
                    ErrorCode.TEXT_EXTRACTION_FAILED,
                    ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                    e
            );

        }
    }
}