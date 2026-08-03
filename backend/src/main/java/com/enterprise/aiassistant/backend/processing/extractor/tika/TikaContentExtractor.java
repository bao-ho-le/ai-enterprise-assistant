package com.enterprise.aiassistant.backend.processing.extractor.tika;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Extraction dùng chung cho PDF/DOCX/TXT: chạy Tika một lần, lấy cả cấu trúc lẫn text.
@Component
@RequiredArgsConstructor
public class TikaContentExtractor {

    private final ProcessingMapper processingMapper;

    public ExtractedText extract(InputStream inputStream, String fileName) {

        try {

            TikaStructuredContentHandler handler = new TikaStructuredContentHandler();

            Metadata metadata = new Metadata();

            if (fileName != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }

            new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());

            List<DocumentElement> elements = handler.getElements();

            // Dựng lại pages từ elements để giữ tương thích ngược với field pages cũ.
            List<String> pages = toPages(elements);

            return processingMapper.toExtractedText(
                    String.join("\n\n", pages),
                    pages,
                    elements,
                    ExtractionMethod.DIRECT_TEXT
            );

        } catch (Exception e) {

            throw new ProcessingException(
                    ErrorCode.TEXT_EXTRACTION_FAILED,
                    ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                    e
            );
        }
    }

    // Gộp các element cùng pageNumber thành 1 phần tử pages, giữ nguyên thứ tự trang.
    private List<String> toPages(List<DocumentElement> elements) {

        Map<Integer, List<DocumentElement>> elementsByPage = elements.stream()
                .collect(Collectors.groupingBy(
                        DocumentElement::getPageNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<String> pages = new ArrayList<>();

        for (List<DocumentElement> pageElements : elementsByPage.values()) {

            pages.add(pageElements.stream()
                    .map(DocumentElement::getContent)
                    .collect(Collectors.joining("\n")));
        }

        return pages;
    }
}
