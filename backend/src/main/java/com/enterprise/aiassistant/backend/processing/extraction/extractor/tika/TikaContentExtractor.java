package com.enterprise.aiassistant.backend.processing.extraction.extractor.tika;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.helper.ExtractionHelper;
import com.enterprise.aiassistant.backend.processing.extraction.mapper.ExtractionMapper;
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

    private final ExtractionMapper extractionMapper;

    private final ExtractionHelper extractionHelper;

    public ExtractedText extract(InputStream inputStream, String fileName) {

        try {

            TikaStructuredContentHandler handler = new TikaStructuredContentHandler(extractionHelper, extractionMapper);

            // Cung cấp metadata cho Tika trước khi, đại loại nó sẽ cung cấp dạng dữ liệu set:
            // ResourceName = "report.pdf"
            Metadata metadata = new Metadata();
            if (fileName != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }

            // Có giải thích cơ chế hoạt đông của Tika Parser ở dưới cùng
            new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());

            List<DocumentElement> elements = handler.getElements();

            // Dựng lại pages từ elements để giữ tương thích ngược với field pages cũ.
            List<String> pages = extractionHelper.toPages(elements);

            return extractionMapper.toExtractedText(
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
}

    /*

     Handler đóng vai trò như một listener của SAX Parser.

     AutoDetectParser nhận InputStream và đọc dữ liệu theo cơ chế streaming,
     tức là đọc từng khối (buffer) byte thay vì tải toàn bộ file vào RAM.
     Kích thước mỗi lần đọc không cố định, phụ thuộc vào implementation của
     parser và InputStream (thường khoảng vài KB như 4KB hoặc 8KB).

     Sau khi nhận đủ dữ liệu cần thiết, AutoDetectParser tự nhận diện định dạng
     tài liệu (PDF, DOCX, TXT,...) và chọn parser phù hợp.

     Mỗi parser sẽ phân tích cấu trúc gốc của tài liệu rồi chuẩn hóa thành một
     luồng XHTML thống nhất. Ví dụ:
     - PDF : suy ra page, paragraph, heading từ cấu trúc và thông tin định dạng.
     - DOCX: đọc trực tiếp XML (<w:p>, Heading style, table,...).
     - TXT : không có cấu trúc nên thường chỉ sinh các <p> đơn giản.

     Trong quá trình parser sinh XHTML, SAX sẽ phát sinh các callback đến
     Handler:
     - startElement(): gặp thẻ mở (<p>, <h1>, <div>, ...)
     - characters() : đọc được nội dung text bên trong thẻ
     - endElement() : gặp thẻ đóng (</p>, </h1>, </div>, ...)

     Nhờ cơ chế event-driven này, handler chỉ cần lắng nghe các callback để xây
     dựng DocumentElement, không cần tự phân tích cấu trúc của từng loại tài liệu
     hay đọc toàn bộ file vào bộ nhớ.

     */