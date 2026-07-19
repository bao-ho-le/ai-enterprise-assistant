package com.enterprise.aiassistant.backend.processing.extractor;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ProcessingException;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class ExcelTextExtractor implements TextExtractor {

    private static final String XLSX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String XLS_MIME_TYPE =
            "application/vnd.ms-excel";

    private final ProcessingMapper processingMapper;

    @Override
    public boolean supports(String mimeType) {
        return XLSX_MIME_TYPE.equalsIgnoreCase(mimeType)
                || XLS_MIME_TYPE.equalsIgnoreCase(mimeType);
    }

    @Override
    public ExtractedText extract(Resource resource) {

        try (
                InputStream inputStream = resource.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)
        ) {

            // Extract all worksheets into a single text
            String content = extractWorkbookContent(workbook);

            return processingMapper.toExtractedText(
                    content,
                    ExtractionMethod.DIRECT_TEXT
            );

        } catch (IOException e) {

            throw new ProcessingException(
                    ErrorCode.TEXT_EXTRACTION_FAILED,
                    ErrorCode.TEXT_EXTRACTION_FAILED.getMessage(),
                    e
            );
        }
    }

    // Extract text from every sheet in the workbook.
    private String extractWorkbookContent(Workbook workbook) {

        StringBuilder content = new StringBuilder();

        for (Sheet sheet : workbook) {
            content.append(extractSheet(sheet));
        }

        return content.toString();
    }

    // Extract text from a single worksheet.
    private String extractSheet(Sheet sheet) {

        StringBuilder sheetContent = new StringBuilder();

        sheetContent.append("Sheet: ")
                .append(sheet.getSheetName())
                .append(System.lineSeparator());

        DataFormatter formatter = new DataFormatter();

        for (Row row : sheet) {

            String rowContent = extractRow(row, formatter);

            if (!rowContent.isBlank()) {
                sheetContent.append(rowContent)
                        .append(System.lineSeparator());
            }
        }

        sheetContent.append(System.lineSeparator());

        return sheetContent.toString();
    }

    // Extract text from a single row.
    private String extractRow(Row row, DataFormatter formatter) {

        StringBuilder rowContent = new StringBuilder();

        for (Cell cell : row) {

            String value = formatter.formatCellValue(cell);

            if (!value.isBlank()) {
                rowContent
                        .append(value)
                        .append('\t');
            }
        }

        return rowContent.toString();
    }
}