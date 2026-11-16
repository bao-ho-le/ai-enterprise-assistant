package com.enterprise.aiassistant.backend.processing.chunking.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.chunking.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.chunking.strategy.ChunkingStrategy;
import com.enterprise.aiassistant.backend.processing.chunking.strategy.ChunkingStrategyFactory;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChunkingServiceImpl}: input validation, strategy delegation and error
 * translation boundaries.
 */
class ChunkingServiceImplTest {

    private final ChunkingStrategyFactory factory = mock(ChunkingStrategyFactory.class);

    private final ChunkingStrategy strategy = mock(ChunkingStrategy.class);

    private ChunkingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChunkingServiceImpl(factory);
    }

    @Test
    void shouldRejectMissingExtension() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.chunk(validText(), null)),
                ErrorCode.DOCUMENT_CHUNKING_FAILED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.chunk(validText(), "  ")),
                ErrorCode.DOCUMENT_CHUNKING_FAILED
        );

        verifyNoInteractions(factory);
    }

    @Test
    void shouldRejectMissingExtractedText() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.chunk(null, "pdf")),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.chunk(ExtractedText.builder().build(), "pdf")),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.chunk(ExtractedText.builder().elements(List.of()).build(), "pdf")),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );

        verifyNoInteractions(factory);
    }

    @Test
    void shouldDelegateToResolvedStrategy() {

        ExtractedText text = validText();
        List<TextChunk> expected = List.of(TextChunk.builder().chunkIndex(0).content("a").build());

        when(factory.getStrategy("pdf")).thenReturn(strategy);
        when(strategy.chunk(text)).thenReturn(expected);

        assertThat(service.chunk(text, "pdf")).isSameAs(expected);
        verify(strategy).chunk(text);
    }

    @Test
    void shouldPropagateUnsupportedTypeWithoutWrapping() {

        when(factory.getStrategy("png"))
                .thenThrow(new com.enterprise.aiassistant.backend.common.exception.business_exception
                        .ProcessingException(ErrorCode.UNSUPPORTED_FILE_TYPE));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.chunk(validText(), "png")),
                ErrorCode.UNSUPPORTED_FILE_TYPE
        );
    }

    @Test
    void shouldWrapStrategyFailures() {

        when(factory.getStrategy("pdf")).thenReturn(strategy);
        when(strategy.chunk(any())).thenThrow(new IllegalStateException("boom"));

        Throwable thrown = catchThrowable(() -> service.chunk(validText(), "pdf"));

        ErrorCodeAssertions.assertBusinessError(thrown, ErrorCode.DOCUMENT_CHUNKING_FAILED);
        assertThat(thrown.getCause()).hasMessage("boom");
    }

    private static ExtractedText validText() {

        return ExtractedText.builder()
                .content("text")
                .elements(List.of(DocumentElement.builder()
                        .type(ElementType.PARAGRAPH)
                        .content("text")
                        .pageNumber(1)
                        .build()))
                .build();
    }
}
