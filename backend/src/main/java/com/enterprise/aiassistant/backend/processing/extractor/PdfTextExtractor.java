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
import java.util.ArrayList;
import java.util.List;

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

            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }

            String content = String.join("\n\n", pages);

            return processingMapper.toExtractedText(content, pages, ExtractionMethod.DIRECT_TEXT);

        } catch (IOException e) {

            throw new ProcessingException(
                    ErrorCode.TEXT_EXTRACTION_FAILED,
                    ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                    e
            );

        }
    }
}