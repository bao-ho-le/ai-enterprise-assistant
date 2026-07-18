package com.enterprise.aiassistant.backend.processing.dto;

import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedText {

    private String content;

    private ExtractionMethod extractionMethod;

    private String language;
}
