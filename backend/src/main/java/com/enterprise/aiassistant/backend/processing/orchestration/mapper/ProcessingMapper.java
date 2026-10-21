package com.enterprise.aiassistant.backend.processing.orchestration.mapper;

import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import static com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper.*;


@Component
public class ProcessingMapper {

    public DocumentText toDocumentText(DocumentVersion version, ExtractedText extractedText) {
        return DocumentText.builder()
                .documentVersion(version)
                .content(extractedText.getContent())
                .extractionMethod(extractedText.getExtractionMethod())
                .language(extractedText.getLanguage())
                .build();
    }

    public DocumentChunk toDocumentChunk(DocumentVersion version, TextSegment segment) {
        Metadata metadata = segment.metadata();
        return DocumentChunk.builder()
                .documentVersion(version)
                .chunkIndex(metadata.getInteger(CHUNK_INDEX))
                .content(segment.text())
                .pageNumber(metadata.getInteger(PAGE_NUMBER))
                .startChar(metadata.getInteger(START_CHAR))
                .endChar(metadata.getInteger(END_CHAR))
                .tokenCount(metadata.getInteger(TOKEN_COUNT))
                .build();
    }
}
