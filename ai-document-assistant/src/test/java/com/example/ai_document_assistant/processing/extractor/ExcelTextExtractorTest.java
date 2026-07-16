package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelTextExtractorTest {

    private final ExcelTextExtractor extractor = new ExcelTextExtractor();

    @Test
    void supports_shouldReturnTrue_forXlsxMimeType() {
        assertTrue(extractor.supports(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void extract_shouldReturnCorrectContent() throws IOException {
        byte[] fileBytes = createSampleExcel("Hello from Excel test");

        ExtractedText result = extractor.extract(fileBytes);

        assertTrue(result.content().contains("Hello from Excel test"));
        assertEquals("APACHE_POI_EXCEL", result.extractionMethod());
    }

    private byte[] createSampleExcel(String text) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(text);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
