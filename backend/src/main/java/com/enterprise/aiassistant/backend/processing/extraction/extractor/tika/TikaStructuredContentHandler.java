package com.enterprise.aiassistant.backend.processing.extraction.extractor.tika;

import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import com.enterprise.aiassistant.backend.processing.extraction.helper.ExtractionHelper;
import com.enterprise.aiassistant.backend.processing.extraction.mapper.ExtractionMapper;
import lombok.Getter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

// Không khai báo @Component cho handler này
// @Component được khai báo cho các Service vì các class Service cần được singleton
// Trong khi TikaStructuredContentHandler sẽ được tạo mới và gọi mới cho mỗi thread
// xử lí document nếu có nhiều thread, mỗi instance sẽ cần mang các state riêng như buffer, openTagName,...
public class TikaStructuredContentHandler extends DefaultHandler {

    private static final String PAGE_DIV_CLASS = "page";

    private final ExtractionHelper extractionHelper;

    private final ExtractionMapper extractionMapper;

    @Getter
    private final List<DocumentElement> elements = new ArrayList<>();

    private final StringBuilder buffer = new StringBuilder();

    private String openTagName;

    private ElementType openType;

    private Integer openHeadingLevel;

    private int pageNumber = 1;

    private int pageCount = 0;

    public TikaStructuredContentHandler(ExtractionHelper extractionHelper, ExtractionMapper extractionMapper) {
        this.extractionHelper = extractionHelper;
        this.extractionMapper = extractionMapper;
    }

    @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {

        // Lưu ý, với <div> thì chỉ đếm số page thôi, chỉ có <p>, <h> mới được lưu vào DocumentElement
        String tagName = extractionHelper.resolveTagName(localName, qName);

        // Tika bọc nội dung mỗi trang PDF trong <div class="page">.
        // Đây chỉ là marker đánh dấu ranh giới trang, không phải nội dung tài liệu.
        // Khi gặp marker này, tăng pageNumber để các heading/paragraph tiếp theo
        // được gắn đúng số trang. DOCX/TXT không có marker này nên pageNumber luôn là 1.
        if ("div".equals(tagName) && PAGE_DIV_CLASS.equals(attributes.getValue("class"))) {
            pageCount++;
            pageNumber = pageCount;
            return;
        }

        // Bỏ qua thẻ lồng bên trong <p>, <h> hoặc <div> để không cắt đôi nội dung.
        // Các thẻ lồng bên trong này thường là các thẻ định dạng như <b> (in đậm) nên cần được bỏ qua
        if (openTagName != null) {
            return;
        }

        Integer headingLevel = extractionHelper.resolveHeadingLevel(tagName);

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
    public void endElement(String uri, String localName, String qName) {

        String tagName = extractionHelper.resolveTagName(localName, qName);

        // !openTagName.equals(tagName) dùng để check thẻ đang đóng không phải thẻ đã mở (tức không thuộc <p>, <h>)
        // Nó có thể là các thẻ định dạng (<b>,...)
        if (openTagName == null || !openTagName.equals(tagName)) {
            return;
        }

        String content = buffer.toString().trim();

        if (!content.isEmpty()) {
            elements.add(
                    extractionMapper.toDocumentElement(openType, content, openHeadingLevel, pageNumber)
            );
        }

        buffer.setLength(0);
        openTagName = null;
        openType = null;
        openHeadingLevel = null;
    }


    @Override
    public void characters(char[] chars, int start, int length) {

        // Kiểm tra chỉ lấy text của <p>, <h>. Tránh lấy các khoảng trống như xuống dòng
        if (openTagName != null) {
            buffer.append(chars, start, length);
        }
    }

}
