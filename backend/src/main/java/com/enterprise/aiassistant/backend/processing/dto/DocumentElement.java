package com.enterprise.aiassistant.backend.processing.dto;

import com.enterprise.aiassistant.backend.processing.enums.ElementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentElement {

    private ElementType type;

    private String content;

    // null nếu không phải HEADING (1 = h1, 2 = h2, ...)
    private Integer headingLevel;

    // PDF: số trang thật; DOCX/TXT: luôn 1; Excel: số thứ tự sheet (1-based).
    private Integer pageNumber;
}
