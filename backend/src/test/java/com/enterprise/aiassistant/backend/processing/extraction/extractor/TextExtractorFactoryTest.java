package com.enterprise.aiassistant.backend.processing.extraction.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TextExtractorFactory} MIME-type routing.
 */
class TextExtractorFactoryTest {

    @Test
    void shouldReturnFirstExtractorThatSupportsMimeType() {

        TextExtractor pdf = mock(TextExtractor.class);
        TextExtractor txt = mock(TextExtractor.class);

        when(pdf.supports("application/pdf")).thenReturn(true);
        when(txt.supports("text/plain")).thenReturn(true);

        TextExtractorFactory factory = new TextExtractorFactory(List.of(pdf, txt));

        assertThat(factory.getExtractor("application/pdf")).isSameAs(pdf);
        assertThat(factory.getExtractor("text/plain")).isSameAs(txt);
    }

    @Test
    void shouldRejectUnknownMimeType() {

        TextExtractor pdf = mock(TextExtractor.class);
        when(pdf.supports("image/png")).thenReturn(false);

        TextExtractorFactory factory = new TextExtractorFactory(List.of(pdf));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> factory.getExtractor("image/png")),
                ErrorCode.TEXT_EXTRACTOR_NOT_FOUND
        );
    }

    @Test
    void shouldRejectNullMimeTypeWhenNoExtractorSupportsIt() {

        TextExtractor pdf = mock(TextExtractor.class);
        when(pdf.supports(null)).thenReturn(false);

        TextExtractorFactory factory = new TextExtractorFactory(List.of(pdf));

        assertThat(catchThrowable(() -> factory.getExtractor(null))).isNotNull();
    }
}
