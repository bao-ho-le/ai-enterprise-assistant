package com.enterprise.aiassistant.backend.processing.chunking.strategy;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.chunking.mapper.ChunkingMapper;
import com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter.CHUNK_SIZE;
import static com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter.MAX_HEADING_BREADCRUMB_DEPTH;

@Component
@RequiredArgsConstructor
public class DocumentChunkingStrategy implements ChunkingStrategy {

    private final ChunkingMapper chunkingMapper;

    private final SlidingWindowSplitter slidingWindowSplitter;

    @Override
    public List<TextChunk> chunk(ExtractedText extractedText) {

        List<TextChunk> chunks = new ArrayList<>();

        // Group toàn bộ DocumentElement theo page
        Map<Integer, List<DocumentElement>> elementsByPage = extractedText.getElements().stream()
                .collect(Collectors.groupingBy(
                        DocumentElement::getPageNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int chunkIndex = 0;

        // Duyệt từng trang để chunk
        for (Map.Entry<Integer, List<DocumentElement>> page : elementsByPage.entrySet()) {

            // chunkPage() trả về chunkIndex kế tiếp sau khi xử lý xong một trang,
            // để trang sau tiếp tục đánh số chunk liên tục trên toàn bộ document.
            chunkIndex = chunkPage(page.getKey(), page.getValue(), chunkIndex, chunks);
        }

        return chunks;
    }


    // Trả về chunkIndex kế tiếp để trang sau chạy tiếp, không reset về 0.
    private int chunkPage(
            int pageNumber,
            List<DocumentElement> pageElements,
            int startChunkIndex,
            List<TextChunk> chunks
    ) {

        int chunkIndex = startChunkIndex;

        StringBuilder buffer = new StringBuilder();

        // HashMap nó sẽ không lưu được thứ tự, dùng TreeMap để lưu được thứ tự:
        // Ví dụ:
        // 1 -> Java
        // 2 -> Collection
        // 3 -> List
        TreeMap<Integer, String> headingByLevel = new TreeMap<>();

        // Vị trí ký tự trong text của trang (các element nối lại bằng "\n").
        int cursor = 0;
        int bufferStart = 0;

        for (DocumentElement element : pageElements) {

            String content = element.getContent();

            int elementStart = cursor;
            cursor += content.length() + 1;

            // Nếu là heading thì cập nhật breadcrumb theo level rồi lặp tiếp.
            // Heading mới ở level L kết thúc phạm vi hiệu lực của mọi heading con
            // (level > L) thuộc nhánh cũ, nên phải xóa các level đó khỏi breadcrumb.
            if (element.getType() == ElementType.HEADING) {
                int headingLevel = element.getHeadingLevel();
                headingByLevel.put(headingLevel, content);
                headingByLevel.tailMap(headingLevel, false).clear();
                continue;
            }

            // Dựng breadcrumb tại thời điểm gặp paragraph này, vì breadcrumb chỉ thay đổi
            // khi gặp heading mới ở trên.
            String breadcrumb = buildBreadcrumb(headingByLevel);

            // Paragraph tự nó đã vượt CHUNK_SIZE -> flush buffer rồi cắt riêng bằng sliding window.
            if (content.length() > CHUNK_SIZE) {

                if (!buffer.isEmpty()) {
                    chunks.add(chunkingMapper.toTextChunk(
                            chunkIndex++, buffer.toString(), pageNumber, bufferStart, elementStart));
                    buffer.setLength(0);
                }

                for (String piece : slidingWindowSplitter.split(content)) {

                    // Thêm breadcrumb heading vào đầu chunk.
                    // Các paragraph dài được SlidingWindowSplitter cắt thành nhiều phần,
                    // nên cần giữ lại breadcrumb để mỗi chunk vẫn chứa đầy đủ ngữ cảnh khi embedding.
                    String pieceContent = breadcrumb != null
                            ? breadcrumb + "\n" + piece
                            : piece;

                    chunks.add(chunkingMapper.toTextChunk(
                            chunkIndex++, pieceContent, pageNumber, elementStart, elementStart + piece.length()));
                }

                continue;
            }

            // Nếu thêm paragraph hiện tại vào buffer sẽ vượt quá giới hạn chunk,
            // flush nội dung đã gom trước đó trong buffer thành một chunk.
            if (!buffer.isEmpty() && buffer.length() + content.length() + 1 > CHUNK_SIZE) {

                chunks.add(chunkingMapper.toTextChunk(
                        chunkIndex++, buffer.toString(), pageNumber, bufferStart, elementStart));
                buffer.setLength(0);
            }

            // Nếu buffer đang rỗng, nghĩa là đang bắt đầu chunk mới thì cần thêm breadcrumb vào
            if (buffer.isEmpty()) {

                bufferStart = elementStart;

                if (breadcrumb != null) {
                    buffer.append(breadcrumb).append('\n');
                }

            } else {
                buffer.append('\n');
            }

            buffer.append(content);
        }

        // buffer có thể vẫn còn nội dung chưa đủ lớn để trigger flush trong vòng lặp.
        // Cần tạo chunk cuối cùng để không bỏ sót phần text còn lại.
        if (!buffer.isEmpty()) {
            chunks.add(chunkingMapper.toTextChunk(
                    chunkIndex++, buffer.toString(), pageNumber, bufferStart, cursor));
        }

        return chunkIndex;
    }

    // Ghép các heading đang hiệu lực (level tăng dần) thành breadcrumb "cha > con".
    // Vượt quá MAX_HEADING_BREADCRUMB_DEPTH thì bỏ bớt từ phía tổ tiên xa nhất,
    // vì heading càng gần cấp hiện tại càng mang tính phân biệt cao.
    private String buildBreadcrumb(Map<Integer, String> headingByLevel) {

        if (headingByLevel.isEmpty()) {
            return null;
        }

        List<String> headings = new ArrayList<>(headingByLevel.values());

        int dropCount = headings.size() - MAX_HEADING_BREADCRUMB_DEPTH;

        if (dropCount > 0) {
            headings = headings.subList(dropCount, headings.size());
        }

        return String.join(" > ", headings);
    }
}
