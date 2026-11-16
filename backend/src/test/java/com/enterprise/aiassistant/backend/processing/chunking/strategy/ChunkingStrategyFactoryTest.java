package com.enterprise.aiassistant.backend.processing.chunking.strategy;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ChunkingStrategyFactory} extension routing.
 */
class ChunkingStrategyFactoryTest {

    private final DocumentChunkingStrategy documentStrategy = mock(DocumentChunkingStrategy.class);

    private final SpreadsheetChunkingStrategy spreadsheetStrategy = mock(SpreadsheetChunkingStrategy.class);

    private final ChunkingStrategyFactory factory =
            new ChunkingStrategyFactory(documentStrategy, spreadsheetStrategy);

    @Test
    void shouldRouteDocumentExtensionsToDocumentStrategy() {

        assertThat(factory.getStrategy("pdf")).isSameAs(documentStrategy);
        assertThat(factory.getStrategy("docx")).isSameAs(documentStrategy);
        assertThat(factory.getStrategy("txt")).isSameAs(documentStrategy);
        assertThat(factory.getStrategy("doc")).isSameAs(documentStrategy);
    }

    @Test
    void shouldRouteSpreadsheetExtensionsToSpreadsheetStrategy() {

        assertThat(factory.getStrategy("xlsx")).isSameAs(spreadsheetStrategy);
        assertThat(factory.getStrategy("xls")).isSameAs(spreadsheetStrategy);
    }

    @Test
    void shouldIgnoreExtensionCase() {

        assertThat(factory.getStrategy("PDF")).isSameAs(documentStrategy);
        assertThat(factory.getStrategy("XLSX")).isSameAs(spreadsheetStrategy);
    }

    @Test
    void shouldRejectUnsupportedExtension() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> factory.getStrategy("png")),
                ErrorCode.UNSUPPORTED_FILE_TYPE
        );
    }

    @Test
    void shouldFailOnNullExtension() {

        assertThat(catchThrowable(() -> factory.getStrategy(null)))
                .isInstanceOf(NullPointerException.class);
    }
}
