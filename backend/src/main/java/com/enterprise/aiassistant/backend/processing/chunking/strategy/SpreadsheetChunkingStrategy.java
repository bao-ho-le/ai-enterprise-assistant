package com.enterprise.aiassistant.backend.processing.chunking.strategy;

import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.chunking.mapper.ChunkingMapper;
import com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter.CHUNK_SIZE;

@Component
@RequiredArgsConstructor
public class SpreadsheetChunkingStrategy implements ChunkingStrategy {

    private static final String ROW_SEPARATOR = "\n";

    private final ChunkingMapper chunkingMapper;

    private final SlidingWindowSplitter slidingWindowSplitter;

    // Gộp row theo từng sheet, mỗi chunk luôn kèm header row để không mất ngữ nghĩa cột.
    // chunkIndex tăng liên tục xuyên suốt cả document.
    @Override
    public List<TextChunk> chunk(ExtractedText extractedText) {

        List<TextChunk> chunks = new ArrayList<>();

        // pageNumber của element Excel chính là sheet index (xem ExcelTextExtractor).
        Map<Integer, List<DocumentElement>> rowsBySheet = extractedText.getElements().stream()
                .collect(Collectors.groupingBy(
                        DocumentElement::getPageNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int chunkIndex = 0;

        // Duyệt từng sheet để chunk
        for (Map.Entry<Integer, List<DocumentElement>> sheet : rowsBySheet.entrySet()) {

            // chunkSheet() trả về chunkIndex kế tiếp sau khi xử lý xong một sheet,
            // để sheet sau tiếp tục đánh số chunk liên tục trên toàn bộ document.
            chunkIndex = chunkSheet(sheet.getKey(), sheet.getValue(), chunkIndex, chunks);
        }

        return chunks;
    }

    // Trả về chunkIndex kế tiếp để sheet sau chạy tiếp, không reset về 0.
    private int chunkSheet(
            int sheetNumber,
            List<DocumentElement> sheetRows,
            int startChunkIndex,
            List<TextChunk> chunks
    ) {

        int chunkIndex = startChunkIndex;

        // Row đầu tiên luôn là header, dùng làm base cho buffer và mọi piece khi split.
        String header = sheetRows.get(0).getContent();

        StringBuilder buffer = new StringBuilder(header);

        // Vị trí ký tự trong text của sheet (các row nối lại bằng ROW_SEPARATOR).
        int cursor = header.length() + ROW_SEPARATOR.length();
        int bufferStart = 0;

        for (int i = 1; i < sheetRows.size(); i++) {

            String row = sheetRows.get(i).getContent();

            int rowStart = cursor;
            cursor += row.length() + ROW_SEPARATOR.length();

            // Row tự nó đã vượt CHUNK_SIZE: flush buffer rồi cắt riêng, mỗi piece vẫn ghép header.
            if (row.length() > CHUNK_SIZE) {

                // Đảm bảo chỉ flush khi buffer hiện có lưu nội dung, chứ không chỉ lưu mỗi heading
                if (buffer.length() > header.length()) {
                    chunks.add(chunkingMapper.toTextChunk(
                            chunkIndex++, buffer.toString(), sheetNumber, bufferStart, rowStart));
                }

                // Mỗi piece vẫn được ghép header ở đầu để giữ ngữ nghĩa cột,
                // dù row bị SlidingWindowSplitter cắt thành nhiều phần.
                for (String piece : slidingWindowSplitter.split(row)) {
                    chunks.add(chunkingMapper.toTextChunk(
                            chunkIndex++,
                            header + ROW_SEPARATOR + piece,
                            sheetNumber,
                            rowStart,
                            rowStart + piece.length()));
                }

                // Reset buffer về header để chunk kế tiếp tiếp tục đúng ngữ cảnh.
                buffer.setLength(0);
                buffer.append(header);
                bufferStart = cursor;

                continue;
            }

            // Nếu thêm row hiện tại vào buffer sẽ vượt quá giới hạn chunk,
            // flush nội dung đã gom trước đó trong buffer thành một chunk.
            if (buffer.length() + row.length() + ROW_SEPARATOR.length() > CHUNK_SIZE) {

                chunks.add(chunkingMapper.toTextChunk(
                        chunkIndex++, buffer.toString(), sheetNumber, bufferStart, rowStart));

                // Bắt đầu chunk mới, vẫn giữ header để không mất ngữ nghĩa cột.
                buffer.setLength(0);
                buffer.append(header);
                bufferStart = rowStart;
            }

            buffer.append(ROW_SEPARATOR).append(row);
        }

        // Chỉ flush khi còn row chưa ghi
        // hoặc sheet chỉ có mỗi header thì vẫn nên tạo chunk cho sheet này
        if (buffer.length() > header.length() || chunkIndex == startChunkIndex) {
            chunks.add(chunkingMapper.toTextChunk(
                    chunkIndex++, buffer.toString(), sheetNumber, bufferStart, cursor));
        }

        return chunkIndex;
    }
}