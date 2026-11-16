package com.enterprise.aiassistant.backend.processing.extraction.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Unit tests for {@link ExtractionHelper}: empty-text guard, SAX tag normalization, heading
 * resolution and page grouping.
 */
class ExtractionHelperTest {

    private final ExtractionHelper helper = new ExtractionHelper();

    @Test
    void shouldRejectNullExtractedText() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateExtractedText(null)),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );
    }

    @Test
    void shouldRejectEmptyOrBlankContent() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateExtractedText(
                        ExtractedText.builder().content("").build())),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateExtractedText(
                        ExtractedText.builder().content("  \n ").build())),
                ErrorCode.DOCUMENT_TEXT_EMPTY
        );
    }

    @Test
    void shouldAcceptNonBlankContent() {

        assertThat(catchThrowable(() -> helper.validateExtractedText(
                ExtractedText.builder().content("text").build()))).isNull();
    }

    @Test
    void shouldPreferLocalNameAndFallBackToQName() {

        assertThat(helper.resolveTagName("p", "html:p")).isEqualTo("p");
        assertThat(helper.resolveTagName("", "html:p")).isEqualTo("html:p");
        assertThat(helper.resolveTagName(null, "h2")).isEqualTo("h2");
    }

    @Test
    void shouldResolveHeadingLevelsOneToSix() {

        assertThat(helper.resolveHeadingLevel("h1")).isEqualTo(1);
        assertThat(helper.resolveHeadingLevel("h3")).isEqualTo(3);
        assertThat(helper.resolveHeadingLevel("h6")).isEqualTo(6);
    }

    @Test
    void shouldReturnNullForNonHeadingTags() {

        assertThat(helper.resolveHeadingLevel(null)).isNull();
        assertThat(helper.resolveHeadingLevel("p")).isNull();
        assertThat(helper.resolveHeadingLevel("h0")).isNull();
        assertThat(helper.resolveHeadingLevel("h7")).isNull();
        assertThat(helper.resolveHeadingLevel("h10")).isNull();
        assertThat(helper.resolveHeadingLevel("div")).isNull();
    }

    @Test
    void shouldJoinElementsPerPageInFirstSeenOrder() {

        List<String> pages = helper.toPages(List.of(
                element("page1 line1", 1),
                element("page2 line1", 2),
                element("page1 line2", 1)
        ));

        assertThat(pages).containsExactly("page1 line1\npage1 line2", "page2 line1");
    }

    @Test
    void shouldReturnNoPagesForNoElements() {

        assertThat(helper.toPages(List.of())).isEmpty();
    }

    private static DocumentElement element(String content, int page) {

        return DocumentElement.builder()
                .type(ElementType.PARAGRAPH)
                .content(content)
                .pageNumber(page)
                .build();
    }
}
