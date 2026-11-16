package com.enterprise.aiassistant.backend.processing.extraction.extractor.tika;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.helper.ExtractionHelper;
import com.enterprise.aiassistant.backend.processing.extraction.mapper.ExtractionMapper;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Unit tests for {@link TikaContentExtractor} using real Tika parsing of in-memory plain-text
 * input, plus failure translation.
 */
class TikaContentExtractorTest {

    private final TikaContentExtractor extractor =
            new TikaContentExtractor(new ExtractionMapper(), new ExtractionHelper());

    @Test
    void shouldExtractParagraphsFromPlainText() {

        ExtractedText extracted = extractor.extract(
                stream("Hello world. This is a plain text document."),
                "notes.txt"
        );

        assertThat(extracted.getContent()).contains("Hello world");
        assertThat(extracted.getElements()).isNotEmpty();
        assertThat(extracted.getPages()).isNotEmpty();
        assertThat(extracted.getExtractionMethod()).isEqualTo(ExtractionMethod.DIRECT_TEXT);
    }

    @Test
    void shouldAttributeAllPlainTextContentToFirstPage() {

        ExtractedText extracted = extractor.extract(stream("line one\nline two"), "notes.txt");

        assertThat(extracted.getPages()).hasSize(1);
    }

    @Test
    void shouldTolerateNullFileName() {

        ExtractedText extracted = extractor.extract(stream("content without file name"), null);

        assertThat(extracted.getContent()).contains("content without file name");
    }

    @Test
    void shouldTranslateTikaFailures() {

        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken stream");
            }
        };

        Throwable thrown = catchThrowable(() -> extractor.extract(failing, "broken.txt"));

        ErrorCodeAssertions.assertBusinessError(thrown, ErrorCode.TEXT_EXTRACTION_FAILED);
    }

    private static ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
