package com.enterprise.aiassistant.backend.processing.chunking;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.processing.chunking.document.DocumentChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.spreadsheet.SpreadsheetChunkingStrategy;
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

            case "pdf", "docx", "txt" -> documentChunkingStrategy;

            case "xlsx", "xls" -> spreadsheetChunkingStrategy;

            default -> throw new ProcessingException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE,
                    ErrorCode.UNSUPPORTED_FILE_TYPE.getMessage() + ": " + fileExtension
            );
        };
    }
}
