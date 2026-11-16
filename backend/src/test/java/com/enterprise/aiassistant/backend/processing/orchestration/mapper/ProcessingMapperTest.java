package com.enterprise.aiassistant.backend.processing.orchestration.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProcessingMapper}: entity and vector point projections.
 */
class ProcessingMapperTest {

    private final ProcessingMapper mapper = new ProcessingMapper();

    @Test
    void shouldMapExtractedTextToDocumentText() {

        DocumentVersion version = DocumentVersion.builder().id(1L).build();

        ExtractedText extractedText = ExtractedText.builder()
                .content("full text")
                .pages(List.of("page one"))
                .extractionMethod(ExtractionMethod.DIRECT_TEXT)
                .language("en")
                .build();

        DocumentText documentText = mapper.toDocumentText(version, extractedText);

        assertThat(documentText.getDocumentVersion()).isSameAs(version);
        assertThat(documentText.getContent()).isEqualTo("full text");
        assertThat(documentText.getExtractionMethod()).isEqualTo(ExtractionMethod.DIRECT_TEXT);
        assertThat(documentText.getLanguage()).isEqualTo("en");
    }

    @Test
    void shouldMapTextChunkToDocumentChunk() {

        DocumentVersion version = DocumentVersion.builder().id(2L).build();

        TextChunk textChunk = TextChunk.builder()
                .chunkIndex(4)
                .content("chunk text")
                .pageNumber(3)
                .startChar(10)
                .endChar(20)
                .tokenCount(2)
                .build();

        DocumentChunk chunk = mapper.toDocumentChunk(version, textChunk);

        assertThat(chunk.getDocumentVersion()).isSameAs(version);
        assertThat(chunk.getChunkIndex()).isEqualTo(4);
        assertThat(chunk.getContent()).isEqualTo("chunk text");
        assertThat(chunk.getPageNumber()).isEqualTo(3);
        assertThat(chunk.getStartChar()).isEqualTo(10);
        assertThat(chunk.getEndChar()).isEqualTo(20);
        assertThat(chunk.getTokenCount()).isEqualTo(2);
    }

    @Test
    void shouldMapChunkAndEmbeddingToVectorPoint() {

        DocumentVersion version = DocumentVersion.builder()
                .id(9L)
                .document(Document.builder().id(5L).build())
                .build();

        DocumentChunk chunk = DocumentChunk.builder()
                .chunkIndex(1)
                .content("chunk")
                .pageNumber(2)
                .startChar(0)
                .endChar(5)
                .tokenCount(1)
                .documentVersion(version)
                .build();
        chunk.setId(77L);

        EmbeddingResult embedding = EmbeddingResult.builder()
                .vector(new float[]{0.1f, 0.2f})
                .dimension(2)
                .model("model-x")
                .inputTokens(3)
                .build();

        VectorPoint point = mapper.toVectorPoint(chunk, embedding);

        assertThat(point.getId()).isEqualTo(77L);
        assertThat(point.getVector()).containsExactly(0.1f, 0.2f);
        assertThat(point.getPayload().getChunkId()).isEqualTo(77L);
        assertThat(point.getPayload().getDocumentId()).isEqualTo(5L);
        assertThat(point.getPayload().getDocumentVersionId()).isEqualTo(9L);
        assertThat(point.getPayload().getChunkIndex()).isEqualTo(1);
        assertThat(point.getPayload().getPageNumber()).isEqualTo(2);
        assertThat(point.getPayload().getEmbeddingModel()).isEqualTo("model-x");
        assertThat(point.getPayload().getContent()).isEqualTo("chunk");
    }
}
