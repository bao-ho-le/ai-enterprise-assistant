package com.enterprise.aiassistant.backend.processing.extraction.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExtractionHelper {

    // Chặn trường hợp file extract ra rỗng (vd PDF scan không có text layer) —
    // nếu không chặn, document sẽ bị đánh READY dù 0 chunk / 0 vector.
    // Dùng BusinessException (không phải ProcessingException) vì đây là lỗi nội
    // dung tất định, retry lại cũng sẽ rỗng như cũ -> fail fast, không tốn 3 lần retry.
    public void validateExtractedText(ExtractedText extractedText) {

        if (extractedText == null
                || extractedText.getContent() == null
                || extractedText.getContent().isBlank()) {

            throw new BusinessException(ErrorCode.DOCUMENT_TEXT_EMPTY);
        }
    }

    // Chuẩn hóa tên tag giữa các SAX parser. Một số parser trả localName (vd: "h1"),
    // một số chỉ trả qName (vd: "html:h1" hoặc "h1"), nên gom về một tagName duy nhất
    // để phần xử lý phía dưới không cần quan tâm namespace.
    public String resolveTagName(String localName, String qName) {
        return localName == null || localName.isEmpty() ? qName : localName;
    }

    // h1..h6 -> 1..6, các thẻ khác trả về null
    public Integer resolveHeadingLevel(String tagName) {

        if (tagName == null
                || tagName.length() != 2
                || tagName.charAt(0) != 'h'
                || tagName.charAt(1) < '1'
                || tagName.charAt(1) > '6') {

            return null;
        }

        return tagName.charAt(1) - '0';
    }

    // Gộp các element cùng pageNumber thành 1 phần tử pages, giữ nguyên thứ tự trang.
    public List<String> toPages(List<DocumentElement> elements) {

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
