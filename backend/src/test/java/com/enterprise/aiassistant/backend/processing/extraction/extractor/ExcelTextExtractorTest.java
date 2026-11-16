package com.enterprise.aiassistant.backend.processing.extraction.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.processing.extraction.dto.DocumentElement;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.enums.ElementType;
import com.enterprise.aiassistant.backend.processing.extraction.mapper.ExtractionMapper;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Unit tests for {@link ExcelTextExtractor} against real in-memory POI workbooks.
 */
class ExcelTextExtractorTest {

    private final ExcelTextExtractor extractor = new ExcelTextExtractor(new ExtractionMapper());

    @Test
    void shouldSupportBothSpreadsheetMimeTypes() {

        assertThat(extractor.supports(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).isTrue();
        assertThat(extractor.supports("application/vnd.ms-excel")).isTrue();
        assertThat(extractor.supports("text/csv")).isFalse();
    }

    @Test
    void shouldExtractRowsPerSheetWithSheetIndexAsPageNumber() throws Exception {

        Resource resource = workbookResource();

        ExtractedText extracted = extractor.extract(resource);

        assertThat(extracted.getPages()).hasSize(2);
        assertThat(extracted.getContent()).contains("Sheet: Employees").contains("Sheet: Totals");

        assertThat(extracted.getElements()).isNotEmpty();
        assertThat(extracted.getElements()).allSatisfy(element ->
                assertThat(element.getType()).isEqualTo(ElementType.TABLE_ROW));

        assertThat(extracted.getElements()).extracting(DocumentElement::getPageNumber)
                .contains(1, 2);

        assertThat(extracted.getElements()).extracting(DocumentElement::getContent)
                .anySatisfy(content -> assertThat(content).contains("Alice").contains("30"));
    }

    @Test
    void shouldSkipBlankCellsAndRows() throws Exception {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Empty");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("");

            ExtractedText extracted = extractor.extract(toResource(workbook));

            assertThat(extracted.getElements()).isEmpty();
            assertThat(extracted.getPages()).hasSize(1);
        }
    }

    @Test
    void shouldTranslateIoFailures() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> extractor.extract(brokenResource())),
                ErrorCode.TEXT_EXTRACTION_FAILED
        );
    }

    private static Resource workbookResource() throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet employees = workbook.createSheet("Employees");
            Row header = employees.createRow(0);
            header.createCell(0).setCellValue("Name");
            header.createCell(1).setCellValue("Age");

            Row first = employees.createRow(1);
            first.createCell(0).setCellValue("Alice");
            first.createCell(1).setCellValue(30);

            Sheet totals = workbook.createSheet("Totals");
            Row totalRow = totals.createRow(0);
            totalRow.createCell(0).setCellValue("Count");
            totalRow.createCell(1).setCellValue(1);

            return toResource(workbook);
        }
    }

    private static Resource toResource(Workbook workbook) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);

        return new ByteArrayResource(out.toByteArray());
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
