package com.enterprise.aiassistant.backend.processing.chunking.service;

import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.chunking.strategy.ChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.strategy.ChunkingStrategyFactory;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.DOCUMENT_CHUNKING_FAILED;
import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.DOCUMENT_TEXT_EMPTY;

@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private final ChunkingStrategyFactory chunkingStrategyFactory;

    @Override
    public List<TextChunk> chunk(ExtractedText extractedText, String fileExtension) {

        validateChunkInput(extractedText, fileExtension);

        // Lấy strategy ngoài try để lỗi unsupported file type không bị bọc thành DOCUMENT_CHUNKING_FAILED.
        ChunkingStrategy chunkingStrategy = chunkingStrategyFactory.getStrategy(fileExtension);

        try {

            return chunkingStrategy.chunk(extractedText);

        } catch (Exception e) {

            throw new ProcessingException(
                    DOCUMENT_CHUNKING_FAILED,
                    DOCUMENT_CHUNKING_FAILED.getMessage(),
                    e
            );
        }
    }

    private void validateChunkInput(ExtractedText extractedText, String fileExtension) {

        if (fileExtension == null || fileExtension.isBlank()) {
            throw new ProcessingException(DOCUMENT_CHUNKING_FAILED);
        }

        if (extractedText == null
                || extractedText.getElements() == null
                || extractedText.getElements().isEmpty()) {

            throw new BusinessException(DOCUMENT_TEXT_EMPTY);
        }
    }
}
