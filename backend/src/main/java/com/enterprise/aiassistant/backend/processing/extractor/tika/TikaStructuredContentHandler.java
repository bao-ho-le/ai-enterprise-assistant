package com.enterprise.aiassistant.backend.processing.extractor.tika;

import com.enterprise.aiassistant.backend.processing.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.enums.ElementType;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

// Bắt XHTML do Tika sinh ra để dựng danh sách DocumentElement theo đúng thứ tự xuất hiện.
public class TikaStructuredContentHandler extends DefaultHandler {

    private static final String PAGE_DIV_CLASS = "page";

    private final List<DocumentElement> elements = new ArrayList<>();

    private final StringBuilder buffer = new StringBuilder();

    private String openTagName;

    private ElementType openType;

    private Integer openHeadingLevel;

    private int pageNumber = 1;

    private int pageCount = 0;

    public List<DocumentElement> getElements() {
        return elements;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        String tagName = resolveTagName(localName, qName);

        if ("div".equals(tagName) && PAGE_DIV_CLASS.equals(attributes.getValue("class"))) {
            // Tika chỉ sinh div.page cho PDF; DOCX/TXT không có nên pageNumber giữ nguyên 1.
            pageCount++;
            pageNumber = pageCount;
            return;
        }

        // Bỏ qua thẻ lồng bên trong (vd <b> trong <p>) để không cắt đôi nội dung.
        if (openTagName != null) {
            return;
        }

        Integer headingLevel = resolveHeadingLevel(tagName);

        if (headingLevel != null) {
            openTagName = tagName;
            openType = ElementType.HEADING;
            openHeadingLevel = headingLevel;
            return;
        }

        if ("p".equals(tagName)) {
            openTagName = tagName;
            openType = ElementType.PARAGRAPH;
            openHeadingLevel = null;
        }
    }

    @Override
    public void characters(char[] chars, int start, int length) {

        if (openTagName != null) {
            buffer.append(chars, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {

        String tagName = resolveTagName(localName, qName);

        if (openTagName == null || !openTagName.equals(tagName)) {
            return;
        }

        String content = buffer.toString().trim();

        if (!content.isEmpty()) {
            elements.add(
                    DocumentElement.builder()
                            .type(openType)
                            .content(content)
                            .headingLevel(openHeadingLevel)
                            .pageNumber(pageNumber)
                            .build()
            );
        }

        buffer.setLength(0);
        openTagName = null;
        openType = null;
        openHeadingLevel = null;
    }

    private String resolveTagName(String localName, String qName) {
        return localName == null || localName.isEmpty() ? qName : localName;
    }

    // h1..h6 -> 1..6, các thẻ khác trả về null
    private Integer resolveHeadingLevel(String tagName) {

        if (tagName == null
                || tagName.length() != 2
                || tagName.charAt(0) != 'h'
                || tagName.charAt(1) < '1'
                || tagName.charAt(1) > '6') {

            return null;
        }

        return tagName.charAt(1) - '0';
    }
}
