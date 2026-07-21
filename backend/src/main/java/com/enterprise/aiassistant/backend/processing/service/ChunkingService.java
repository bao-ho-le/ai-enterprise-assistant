package com.enterprise.aiassistant.backend.processing.service;

import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.DOCUMENT_CHUNKING_FAILED;

@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final ProcessingMapper processingMapper;

    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP_SIZE = 200;


    // Chunk theo từng trang riêng biệt (không cho chunk tràn qua ranh giới trang),
    // chunkIndex vẫn tăng liên tục xuyên suốt cả document để không đụng unique
    // constraint (document_version_id, chunk_index).
    public List<TextChunk> chunk(List<String> pages) {

        try {

            List<TextChunk> chunks = new ArrayList<>();

            int chunkIndex = 0;

            for (int i = 0; i < pages.size(); i++) {

                String pageText = pages.get(i);
                int pageNumber = i + 1;

                if (pageText == null || pageText.isEmpty()) {
                    continue;
                }

                int start = 0;

                while (start < pageText.length()) {

                    int end = Math.min(
                            start + CHUNK_SIZE,
                            pageText.length()
                    );

                    String content = pageText.substring(start, end);

                    chunks.add(
                            processingMapper.toTextChunk(
                                    chunkIndex++,
                                    content,
                                    pageNumber,
                                    start,
                                    end
                            )
                    );

                    // Nếu đã tới cuối trang thì dừng
                    if (end == pageText.length()) {
                        break;
                    }

                    // Lùi lại overlap
                    start = end - OVERLAP_SIZE;
                }
            }

            return chunks;


        } catch (Exception e) {

            throw new ProcessingException(
                    DOCUMENT_CHUNKING_FAILED,
                    DOCUMENT_CHUNKING_FAILED.getMessage(),
                    e
            );
        }
    }
}
