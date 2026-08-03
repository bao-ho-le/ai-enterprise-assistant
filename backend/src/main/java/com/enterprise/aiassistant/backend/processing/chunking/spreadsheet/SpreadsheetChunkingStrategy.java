package com.enterprise.aiassistant.backend.processing.chunking.spreadsheet;

import com.enterprise.aiassistant.backend.processing.chunking.ChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter;
import com.enterprise.aiassistant.backend.processing.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter.CHUNK_SIZE;
import static com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter.OVERLAP_SIZE;

@Component
@RequiredArgsConstructor
public class SpreadsheetChunkingStrategy implements ChunkingStrategy {

    private static final String ROW_SEPARATOR = "\n";

    private final ProcessingMapper processingMapper;

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

        for (Map.Entry<Integer, List<DocumentElement>> sheet : rowsBySheet.entrySet()) {
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

                if (buffer.length() > header.length()) {
                    chunks.add(processingMapper.toTextChunk(
                            chunkIndex++, buffer.toString(), sheetNumber, bufferStart, rowStart));
                }

                for (String piece : slidingWindowSplitter.split(row, CHUNK_SIZE, OVERLAP_SIZE)) {
                    chunks.add(processingMapper.toTextChunk(
                            chunkIndex++,
                            header + ROW_SEPARATOR + piece,
                            sheetNumber,
                            rowStart,
                            rowStart + piece.length()));
                }

                buffer.setLength(0);
                buffer.append(header);
                bufferStart = cursor;

                continue;
            }

            if (buffer.length() + row.length() + ROW_SEPARATOR.length() > CHUNK_SIZE) {

                chunks.add(processingMapper.toTextChunk(
                        chunkIndex++, buffer.toString(), sheetNumber, bufferStart, rowStart));

                buffer.setLength(0);
                buffer.append(header);
                bufferStart = rowStart;
            }

            buffer.append(ROW_SEPARATOR).append(row);
        }

        // Chỉ flush khi còn row chưa ghi, hoặc sheet chỉ có mỗi header (chưa sinh chunk nào).
        if (buffer.length() > header.length() || chunkIndex == startChunkIndex) {
            chunks.add(processingMapper.toTextChunk(
                    chunkIndex++, buffer.toString(), sheetNumber, bufferStart, cursor));
        }

        return chunkIndex;
    }
}
