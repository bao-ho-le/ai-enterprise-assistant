package com.enterprise.aiassistant.backend.processing.extractor.tika;

import com.enterprise.aiassistant.backend.processing.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.enums.ElementType;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikaContentExtractorTest {

    private final TikaContentExtractor tikaContentExtractor =
            new TikaContentExtractor(new ProcessingMapper());

    @Test
    void extractsPlainTextAsSingleParagraphOnPageOne() {

        ExtractedText extractedText = tikaContentExtractor.extract(
                new ByteArrayInputStream("hello tika".getBytes(StandardCharsets.UTF_8)),
                "note.txt"
        );

        List<DocumentElement> elements = extractedText.getElements();

        assertEquals(1, elements.size());
        assertEquals(ElementType.PARAGRAPH, elements.get(0).getType());
        assertEquals(1, elements.get(0).getPageNumber());
        assertTrue(extractedText.getContent().contains("hello tika"));
        assertEquals(1, extractedText.getPages().size());
    }

    @Test
    void assignsRealPageNumbersForPdf() throws Exception {

        ExtractedText extractedText = tikaContentExtractor.extract(
                new ByteArrayInputStream(twoPagePdf()),
                "sample.pdf"
        );

        List<Integer> pageNumbers = extractedText.getElements().stream()
                .map(DocumentElement::getPageNumber)
                .distinct()
                .toList();

        assertEquals(List.of(1, 2), pageNumbers);
        assertEquals(2, extractedText.getPages().size());
        assertTrue(extractedText.getPages().get(0).contains("first page"));
        assertTrue(extractedText.getPages().get(1).contains("second page"));
    }

    private byte[] twoPagePdf() throws Exception {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            writePage(document, "text of the first page");
            writePage(document, "text of the second page");

            document.save(output);

            return output.toByteArray();
        }
    }

    private void writePage(PDDocument document, String text) throws Exception {

        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {

            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(50, 700);
            content.showText(text);
            content.endText();
        }
    }
}
