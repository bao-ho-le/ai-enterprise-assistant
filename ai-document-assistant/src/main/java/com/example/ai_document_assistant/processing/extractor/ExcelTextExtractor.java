package com.example.ai_document_assistant.processing.extractor;

import com.example.ai_document_assistant.processing.dto.ExtractedText;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class ExcelTextExtractor implements TextExtractor {

    private static final String XLSX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XLS_MIME_TYPE = "application/vnd.ms-excel";

    @Override
    public boolean supports(String mimeType) {
        return XLSX_MIME_TYPE.equals(mimeType) || XLS_MIME_TYPE.equals(mimeType);
    }

    @Override
    public ExtractedText extract(byte[] fileBytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            StringBuilder content = new StringBuilder();
            DataFormatter formatter = new DataFormatter();

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                for (Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = formatter.formatCellValue(cell);
                        if (!cellValue.isBlank()) {
                            rowText.append(cellValue).append("\t");
                        }
                    }
                    if (!rowText.isEmpty()) {
                        content.append(rowText).append("\n");
                    }
                }
                content.append("\n");
            }

            return new ExtractedText(content.toString(), "APACHE_POI_EXCEL", null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract text from Excel file", e);
        }
    }
}
