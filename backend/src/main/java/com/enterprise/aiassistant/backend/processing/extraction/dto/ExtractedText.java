package com.enterprise.aiassistant.backend.processing.extraction.dto;

import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedText {

    private String content;

    // Text thuần theo từng trang/sheet, dùng cho page-aware chunking.
    private List<String> pages;

    // Các phần tử cấu trúc (heading/paragraph/table row) theo đúng thứ tự xuất hiện,
    // dùng cho structure-aware chunking.
    private List<DocumentElement> elements;

    private ExtractionMethod extractionMethod;

    private String language;
}
