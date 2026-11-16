package com.enterprise.aiassistant.backend.processing.extraction.service;

import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.extractor.TextExtractor;
import com.enterprise.aiassistant.backend.processing.extraction.extractor.TextExtractorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TextExtractionServiceImpl} delegation.
 */
class TextExtractionServiceImplTest {

    private final TextExtractorFactory factory = mock(TextExtractorFactory.class);

    private final TextExtractionServiceImpl service = new TextExtractionServiceImpl(factory);

    @Test
    void shouldDelegateToExtractorResolvedByMimeType() {

        Resource resource = new ByteArrayResource(new byte[]{1});
        ExtractedText expected = ExtractedText.builder().content("text").build();
        TextExtractor extractor = mock(TextExtractor.class);

        when(factory.getExtractor("application/pdf")).thenReturn(extractor);
        when(extractor.extract(resource)).thenReturn(expected);

        assertThat(service.extract(resource, "application/pdf")).isSameAs(expected);
        verify(extractor).extract(resource);
    }
}
