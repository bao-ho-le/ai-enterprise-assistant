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


    public List<TextChunk> chunk(String text) {

        try {

            List<TextChunk> chunks = new ArrayList<>();

            int start = 0;
            int chunkIndex = 0;


            while (start < text.length()) {

                int end = Math.min(
                        start + CHUNK_SIZE,
                        text.length()
                );


                String content = text.substring(start, end);


                chunks.add(
                        processingMapper.toTextChunk(
                                chunkIndex++,
                                content,
                                start,
                                end
                        )
                );


                // Nếu đã tới cuối text thì dừng
                if (end == text.length()) {
                    break;
                }


                // Lùi lại overlap
                start = end - OVERLAP_SIZE;
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
