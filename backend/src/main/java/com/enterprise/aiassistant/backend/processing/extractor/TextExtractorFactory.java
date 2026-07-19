package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TextExtractorFactory {

    private final List<TextExtractor> extractors;

    public TextExtractor getExtractor(String mimeType) {

        return extractors.stream()
                .filter(extractors -> extractors.supports(mimeType))
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.TEXT_EXTRACTOR_NOT_FOUND,
                                ErrorCode.TEXT_EXTRACTOR_NOT_FOUND.getMessage() + mimeType
                        )
                );

    }
}