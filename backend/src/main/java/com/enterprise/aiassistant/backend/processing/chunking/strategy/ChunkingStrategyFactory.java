package com.enterprise.aiassistant.backend.processing.chunking.strategy;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

// Nơi duy nhất rẽ nhánh theo loại file trong package chunking.
@Component
@RequiredArgsConstructor
public class ChunkingStrategyFactory {

    private final DocumentChunkingStrategy documentChunkingStrategy;

    private final SpreadsheetChunkingStrategy spreadsheetChunkingStrategy;

    public ChunkingStrategy getStrategy(String fileExtension) {

        return switch (fileExtension.toLowerCase(Locale.ROOT)) {

            case "pdf", "docx", "txt", "doc" -> documentChunkingStrategy;

            case "xlsx", "xls" -> spreadsheetChunkingStrategy;

            default -> throw new ProcessingException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE,
                    ErrorCode.UNSUPPORTED_FILE_TYPE.getMessage() + ": " + fileExtension
            );
        };
    }
}
