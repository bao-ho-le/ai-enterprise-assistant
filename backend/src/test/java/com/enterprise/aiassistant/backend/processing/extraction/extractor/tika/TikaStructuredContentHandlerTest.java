package com.enterprise.aiassistant.backend.processing.extraction.extractor.tika;

import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import com.enterprise.aiassistant.backend.processing.extraction.helper.ExtractionHelper;
import com.enterprise.aiassistant.backend.processing.extraction.mapper.ExtractionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TikaStructuredContentHandler} driven directly through SAX callbacks.
 */
class TikaStructuredContentHandlerTest {

    private TikaStructuredContentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TikaStructuredContentHandler(new ExtractionHelper(), new ExtractionMapper());
    }

    @Test
    void shouldCollectParagraphContent() throws Exception {

        start("p");
        characters("hello world");
        end("p");

        assertThat(handler.getElements()).hasSize(1);

        DocumentElement element = handler.getElements().get(0);

        assertThat(element.getType()).isEqualTo(ElementType.PARAGRAPH);
        assertThat(element.getContent()).isEqualTo("hello world");
        assertThat(element.getPageNumber()).isEqualTo(1);
        assertThat(element.getHeadingLevel()).isNull();
    }

    @Test
    void shouldDetectHeadingLevel() throws Exception {

        start("h2");
        characters("Chapter");
        end("h2");

        DocumentElement element = handler.getElements().get(0);

        assertThat(element.getType()).isEqualTo(ElementType.HEADING);
        assertThat(element.getHeadingLevel()).isEqualTo(2);
        assertThat(element.getContent()).isEqualTo("Chapter");
    }

    @Test
    void shouldIncrementPageOnPageMarker() throws Exception {

        pageMarker();

        start("p");
        characters("page one");
        end("p");

        pageMarker();

        start("p");
        characters("page two");
        end("p");

        assertThat(handler.getElements()).extracting(DocumentElement::getPageNumber)
                .containsExactly(1, 2);
    }

    @Test
    void shouldIgnoreNestedInlineTags() throws Exception {

        start("p");
        characters("bold ");
        start("b");
        characters("text");
        end("b");
        characters(" end");
        end("p");

        assertThat(handler.getElements()).hasSize(1);
        assertThat(handler.getElements().get(0).getContent()).isEqualTo("bold text end");
    }

    @Test
    void shouldIgnoreTextOutsideParagraphAndHeading() throws Exception {

        characters("ignored");

        assertThat(handler.getElements()).isEmpty();
    }

    @Test
    void shouldSkipBlankParagraph() throws Exception {

        start("p");
        characters("   ");
        end("p");

        assertThat(handler.getElements()).isEmpty();
    }

    @Test
    void shouldTrimContent() throws Exception {

        start("p");
        characters("  padded  ");
        end("p");

        assertThat(handler.getElements().get(0).getContent()).isEqualTo("padded");
    }

    @Test
    void shouldCollectMultipleElementsInOrder() throws Exception {

        start("h1");
        characters("Title");
        end("h1");

        start("p");
        characters("First");
        end("p");

        start("p");
        characters("Second");
        end("p");

        assertThat(handler.getElements()).extracting(DocumentElement::getContent)
                .containsExactly("Title", "First", "Second");
    }

    @Test
    void shouldResetBufferBetweenElements() throws Exception {

        start("p");
        characters("one");
        end("p");

        start("p");
        characters("two");
        end("p");

        assertThat(handler.getElements()).extracting(DocumentElement::getContent)
                .containsExactly("one", "two");
    }

    @Test
    void shouldIgnoreUnrelatedEndElement() throws Exception {

        start("p");
        characters("kept");
        end("span");

        assertThat(handler.getElements()).isEmpty();

        end("p");

        assertThat(handler.getElements()).hasSize(1);
    }

    private void pageMarker() throws Exception {

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "class", "class", "CDATA", "page");

        handler.startElement("", "div", "div", attributes);
    }

    private void start(String tag) throws Exception {
        handler.startElement("", tag, tag, new AttributesImpl());
    }

    private void end(String tag) throws Exception {
        handler.endElement("", tag, tag);
    }

    private void characters(String text) {
        char[] chars = text.toCharArray();
        handler.characters(chars, 0, chars.length);
    }
}
