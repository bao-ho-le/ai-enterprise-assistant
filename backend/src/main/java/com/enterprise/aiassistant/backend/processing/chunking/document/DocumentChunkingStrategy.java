package com.enterprise.aiassistant.backend.processing.chunking.document;

import com.enterprise.aiassistant.backend.processing.chunking.ChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.support.SlidingWindowSplitter;
import com.enterprise.aiassistant.backend.processing.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.enums.ElementType;
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
public class DocumentChunkingStrategy implements ChunkingStrategy {

    private final ProcessingMapper processingMapper;

    private final SlidingWindowSplitter slidingWindowSplitter;

    // Gom paragraph theo heading gần nhất, không cho chunk tràn qua ranh giới trang,
    // chunkIndex vẫn tăng liên tục xuyên suốt cả document để không đụng unique
    // constraint (document_version_id, chunk_index).
    @Override
    public List<TextChunk> chunk(ExtractedText extractedText) {

        List<TextChunk> chunks = new ArrayList<>();

        Map<Integer, List<DocumentElement>> elementsByPage = extractedText.getElements().stream()
                .collect(Collectors.groupingBy(
                        DocumentElement::getPageNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int chunkIndex = 0;

        for (Map.Entry<Integer, List<DocumentElement>> page : elementsByPage.entrySet()) {
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

        String headingContext = null;

        // Vị trí ký tự trong text của trang (các element nối lại bằng "\n").
        int cursor = 0;
        int bufferStart = 0;

        for (DocumentElement element : pageElements) {

            String content = element.getContent();

            int elementStart = cursor;
            cursor += content.length() + 1;

            if (element.getType() == ElementType.HEADING) {
                headingContext = content;
                continue;
            }

            // Paragraph tự nó đã vượt CHUNK_SIZE: flush buffer rồi cắt riêng bằng sliding window.
            if (content.length() > CHUNK_SIZE) {

                if (!buffer.isEmpty()) {
                    chunks.add(processingMapper.toTextChunk(
                            chunkIndex++, buffer.toString(), pageNumber, bufferStart, elementStart));
                    buffer.setLength(0);
                }

                for (String piece : slidingWindowSplitter.split(content, CHUNK_SIZE, OVERLAP_SIZE)) {
                    chunks.add(processingMapper.toTextChunk(
                            chunkIndex++, piece, pageNumber, elementStart, elementStart + piece.length()));
                }

                continue;
            }

            if (!buffer.isEmpty() && buffer.length() + content.length() + 1 > CHUNK_SIZE) {

                chunks.add(processingMapper.toTextChunk(
                        chunkIndex++, buffer.toString(), pageNumber, bufferStart, elementStart));
                buffer.setLength(0);
            }

            if (buffer.isEmpty()) {

                bufferStart = elementStart;

                if (headingContext != null) {
                    buffer.append(headingContext).append('\n');
                }

            } else {
                buffer.append('\n');
            }

            buffer.append(content);
        }

        if (!buffer.isEmpty()) {
            chunks.add(processingMapper.toTextChunk(
                    chunkIndex++, buffer.toString(), pageNumber, bufferStart, cursor));
        }

        return chunkIndex;
    }
}
