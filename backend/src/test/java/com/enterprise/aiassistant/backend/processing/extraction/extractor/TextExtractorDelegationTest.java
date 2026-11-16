package com.enterprise.aiassistant.backend.processing.extraction.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.extractor.tika.TikaContentExtractor;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Tika-backed PDF/DOCX/TXT extractors: MIME support, delegation to
 * {@link TikaContentExtractor} and I/O failure translation. Real parsing is covered by
 * {@code TikaContentExtractorTest}.
 */
class TextExtractorDelegationTest {

    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final TikaContentExtractor tikaContentExtractor = mock(TikaContentExtractor.class);

    @Test
    void shouldSupportExpectedMimeTypes() {

        assertThat(new PdfTextExtractor(tikaContentExtractor).supports("application/pdf")).isTrue();
        assertThat(new PdfTextExtractor(tikaContentExtractor).supports("APPLICATION/PDF")).isTrue();
        assertThat(new PdfTextExtractor(tikaContentExtractor).supports("text/plain")).isFalse();

        assertThat(new DocxTextExtractor(tikaContentExtractor).supports(DOCX_MIME)).isTrue();
        assertThat(new DocxTextExtractor(tikaContentExtractor).supports("application/pdf")).isFalse();

        assertThat(new TxtTextExtractor(tikaContentExtractor).supports("text/plain")).isTrue();
        assertThat(new TxtTextExtractor(tikaContentExtractor).supports("TEXT/PLAIN")).isTrue();
        assertThat(new TxtTextExtractor(tikaContentExtractor).supports(null)).isFalse();
    }

    @Test
    void shouldDelegatePdfExtractionWithFileName() {

        Resource resource = namedResource("report.pdf", new byte[]{1, 2});
        ExtractedText expected = ExtractedText.builder().content("pdf text").build();

        when(tikaContentExtractor.extract(any(InputStream.class), eq("report.pdf"))).thenReturn(expected);

        assertThat(new PdfTextExtractor(tikaContentExtractor).extract(resource)).isSameAs(expected);
    }

    @Test
    void shouldDelegateDocxExtraction() {

        Resource resource = namedResource("report.docx", new byte[]{1});
        ExtractedText expected = ExtractedText.builder().content("docx text").build();

        when(tikaContentExtractor.extract(any(InputStream.class), eq("report.docx"))).thenReturn(expected);

        assertThat(new DocxTextExtractor(tikaContentExtractor).extract(resource)).isSameAs(expected);
    }

    @Test
    void shouldDelegateTxtExtraction() {

        Resource resource = namedResource("notes.txt", new byte[]{1});
        ExtractedText expected = ExtractedText.builder().content("txt text").build();

        when(tikaContentExtractor.extract(any(InputStream.class), eq("notes.txt"))).thenReturn(expected);

        assertThat(new TxtTextExtractor(tikaContentExtractor).extract(resource)).isSameAs(expected);
    }

    @Test
    void shouldPassNullFileNameWhenResourceHasNone() {

        Resource resource = new ByteArrayResource(new byte[]{1});
        ExtractedText expected = ExtractedText.builder().content("text").build();

        when(tikaContentExtractor.extract(any(InputStream.class), isNull())).thenReturn(expected);

        assertThat(new PdfTextExtractor(tikaContentExtractor).extract(resource)).isSameAs(expected);
        verify(tikaContentExtractor).extract(any(InputStream.class), isNull());
    }

    @Test
    void shouldTranslateIoFailuresForEveryTikaBackedExtractor() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> new PdfTextExtractor(tikaContentExtractor).extract(brokenResource())),
                ErrorCode.TEXT_EXTRACTION_FAILED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> new DocxTextExtractor(tikaContentExtractor).extract(brokenResource())),
                ErrorCode.TEXT_EXTRACTION_FAILED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> new TxtTextExtractor(tikaContentExtractor).extract(brokenResource())),
                ErrorCode.TEXT_EXTRACTION_FAILED
        );
    }

    private static Resource namedResource(String filename, byte[] content) {

        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static Resource brokenResource() {

        return new AbstractResource() {
            @Override
            public String getDescription() {
                return "broken";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("cannot read");
            }
        };
    }
}
