package com.enterprise.aiassistant.backend.processing.extraction.mapper;

import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExtractionMapper {

    public ExtractedText toExtractedText(
            String content,
            List<String> pages,
            List<DocumentElement> elements,
            ExtractionMethod extractionMethod
    ) {
        return ExtractedText.builder()
                .content(content)
                .pages(pages)
                .elements(elements)
                .extractionMethod(extractionMethod)
                .build();
    }

    public DocumentElement toDocumentElement(
            ElementType type,
            String content,
            Integer headingLevel,
            Integer pageNumber
    ) {
        return DocumentElement.builder()
                .type(type)
                .content(content)
                .headingLevel(headingLevel)
                .pageNumber(pageNumber)
                .build();
    }
}
