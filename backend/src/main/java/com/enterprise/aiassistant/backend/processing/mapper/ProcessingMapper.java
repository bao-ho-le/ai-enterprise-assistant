package com.enterprise.aiassistant.backend.processing.mapper;

import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;
import org.springframework.stereotype.Component;




@Component
public class ProcessingMapper {

    public ExtractedText toExtractedText(String content, ExtractionMethod extractionMethod
    ) {
        return ExtractedText.builder()
                .content(content)
                .extractionMethod(extractionMethod)
                .build();
    }

    public TextChunk toTextChunk(
            int chunkIndex,
            String content,
            int startChar,
            int endChar
    ) {

        return TextChunk.builder()
                .chunkIndex(chunkIndex)
                .content(content)
                .startChar(startChar)
                .endChar(endChar)
                .tokenCount(content.length())
                .build();
    }

    public DocumentText toDocumentText(DocumentVersion version, ExtractedText extractedText ) {
        return DocumentText.builder()
                .documentVersion(version)
                .content(extractedText.getContent())
                .extractionMethod(extractedText.getExtractionMethod())
                .language(extractedText.getLanguage())
                .build();
    }

    public DocumentChunk toDocumentChunk(DocumentVersion version, TextChunk textChunk) {
        return DocumentChunk.builder()
                .documentVersion(version)
                .chunkIndex(textChunk.getChunkIndex())
                .content(textChunk.getContent())
                .pageNumber(textChunk.getPageNumber())
                .startChar(textChunk.getStartChar())
                .endChar(textChunk.getEndChar())
                .tokenCount(textChunk.getTokenCount())
                .build();
    }

    public VectorPoint toVectorPoint(DocumentChunk chunk, EmbeddingResult embeddingResult) {

        VectorPayload payload = VectorPayload.builder()
                .chunkId(chunk.getId())
                .documentId(chunk.getDocumentVersion().getDocument().getId())
                .documentVersionId(chunk.getDocumentVersion().getId())
                .chunkIndex(chunk.getChunkIndex())
                .pageNumber(chunk.getPageNumber())
                .startChar(chunk.getStartChar())
                .endChar(chunk.getEndChar())
                .tokenCount(chunk.getTokenCount())
                .embeddingModel(embeddingResult.getModel())
                .content(chunk.getContent())
                .build();

        return VectorPoint.builder()
                .id(String.valueOf(chunk.getId()))
                .vector(embeddingResult.getVector())
                .payload(payload)
                .build();
    }
}