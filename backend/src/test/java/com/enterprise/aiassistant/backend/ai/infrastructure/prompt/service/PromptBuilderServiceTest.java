package com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.FormGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.ReportGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Service đọc file prompt thật trong src/main/resources/prompts, nên test kiểm tra
// prompt có chứa đúng dòng đã điền (contains), không so sánh cả prompt dài.
class PromptBuilderServiceTest {

    private final PromptBuilderService service = new PromptBuilderService();

    // ===================== buildEmailPrompt =====================

    @Test
    void buildEmailPrompt_fullInput_fillsAllFields() {
        EmailGenerationInput input = new EmailGenerationInput();
        input.setRecipient("Phòng nhân sự");
        input.setPurpose("Xin nghỉ phép 2 ngày");
        input.setSender("Lê Văn A");
        input.setTone("Formal");

        String prompt = service.buildEmailPrompt(input);

        assertThat(prompt)
                .contains("Recipient: Phòng nhân sự")
                .contains("Purpose: Xin nghỉ phép 2 ngày")
                .contains("Sender: Lê Văn A")
                .contains("Tone: Formal. Use formal, dignified language.");
    }

    @Test
    void buildEmailPrompt_missingOptionalFields_usesDefaults() {
        EmailGenerationInput input = new EmailGenerationInput();
        input.setPurpose("Xin nghỉ phép");

        String prompt = service.buildEmailPrompt(input);

        assertThat(prompt)
                .contains("Recipient: the recipient")
                .contains("Additional context: None")
                .contains("Sender: [Your Name]")
                .contains("Language: English. Write entirely in English.")
                .contains("Tone: Professional.")
                .contains("Length: Medium.");
    }

    // Câu hướng dẫn có dấu phẩy nên đổi ký tự phân cách của CsvSource sang '|' cho dễ đọc
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Formal     | Use formal, dignified language.",
            "Friendly   | Sound warm and friendly",
            "Persuasive | Focus on persuading the reader",
            "Apologetic | Show genuine empathy",
            "Unknown    | Sound objective, clear, and trustworthy"
    })
    void buildEmailPrompt_tone_addsMatchingToneInstruction(String tone, String expectedInstruction) {
        EmailGenerationInput input = new EmailGenerationInput();
        input.setPurpose("Xin nghỉ phép");
        input.setTone(tone);

        assertThat(service.buildEmailPrompt(input)).contains(expectedInstruction);
    }

    @Test
    void buildEmailPrompt_vietnamese_addsDiacriticsInstruction() {
        EmailGenerationInput input = new EmailGenerationInput();
        input.setPurpose("Xin nghỉ phép");
        input.setLanguage("Vietnamese");

        assertThat(service.buildEmailPrompt(input)).contains("with full diacritics");
    }

    // ===================== buildReportPrompt =====================

    @ParameterizedTest
    @ValueSource(strings = {"Executive", "Executive Leadership"})
    void buildReportPrompt_executiveAudience_addsExecutiveInstruction(String audience) {
        ReportGenerationInput input = reportInput();
        input.setAudience(audience);

        assertThat(service.buildReportPrompt(input, "Nội dung tài liệu")).contains("Write for executives");
    }

    @Test
    void buildReportPrompt_noDates_periodNotSpecified() {
        assertThat(service.buildReportPrompt(reportInput(), "Nội dung tài liệu"))
                .contains("Reporting period: Not specified");
    }

    @Test
    void buildReportPrompt_onlyFromDate_usesEllipsisForToDate() {
        ReportGenerationInput input = reportInput();
        input.setFromDate("2026-01-01");

        assertThat(service.buildReportPrompt(input, "Nội dung tài liệu"))
                .contains("Reporting period: 2026-01-01 to …");
    }

    @Test
    void buildReportPrompt_nullDocumentContext_usesNoSourceMessage() {
        assertThat(service.buildReportPrompt(reportInput(), null)).contains("No source documents attached.");
    }

    @Test
    void buildReportPrompt_longLength_requiresEightSections() {
        ReportGenerationInput input = reportInput();
        input.setLength("Long");

        assertThat(service.buildReportPrompt(input, "Nội dung tài liệu"))
                .contains("exactly these 8 numbered sections");
    }

    // ===================== buildSummaryPrompt =====================

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "true,  Yes",
            "false, No",
            "NULL,  No"
    })
    void buildSummaryPrompt_includeActionItems_writesYesOrNo(Boolean includeActionItems, String expected) {
        SummaryGenerationInput input = new SummaryGenerationInput();
        input.setIncludeActionItems(includeActionItems);

        assertThat(service.buildSummaryPrompt(input, "Nội dung tài liệu"))
                .contains("Include a dedicated action items section: " + expected);
    }

    @Test
    void buildSummaryPrompt_lowercaseStyle_isCaseInsensitive() {
        SummaryGenerationInput input = new SummaryGenerationInput();
        input.setStyle("bullet_points");

        assertThat(service.buildSummaryPrompt(input, "Nội dung tài liệu"))
                .contains("Present the summary as bullet points");
    }

    // ===================== buildFormPrompt =====================

    @Test
    void buildFormPrompt_hasFields_fillsPurposeAndFields() {
        FormGenerationInput input = new FormGenerationInput();
        input.setPurpose("Đơn xin nghỉ phép");
        input.setFields("Họ tên, Ngày nghỉ, Lý do");

        assertThat(service.buildFormPrompt(input))
                .contains("Design a form for the following purpose: Đơn xin nghỉ phép")
                .contains("Desired fields: Họ tên, Ngày nghỉ, Lý do");
    }

    // TODO (bạn tự viết): buildFormPrompt_noFields_usesInferInstruction
    // Chỉ set purpose, không set fields (fields = null)
    // → prompt phải chứa "Desired fields: Infer reasonable fields from the purpose."
    // Gợi ý: làm giống test ngay phía trên, chỉ bỏ dòng setFields.
    @Test
    void buildFormPrompt_noFields_usesInferInstruction() {
        FormGenerationInput input = new FormGenerationInput();
        input.setPurpose("Đơn xin nghỉ phép");

        assertThat(service.buildFormPrompt(input))
                .contains("Design a form for the following purpose: Đơn xin nghỉ phép")
                .contains("Desired fields: Infer reasonable fields from the purpose.");
    }

    // ===================== buildDocumentExcerpts =====================

    @ParameterizedTest
    @NullAndEmptySource
    void buildDocumentExcerpts_noChunks_returnsNoPassagesMessage(List<String> chunks) {
        assertThat(service.buildDocumentExcerpts(chunks)).isEqualTo("No relevant passages were found.");
    }

    @Test
    void buildDocumentExcerpts_chunks_joinsWithSeparator() {
        assertThat(service.buildDocumentExcerpts(List.of("Đoạn 1", "Đoạn 2")))
                .isEqualTo("Đoạn 1\n---\nĐoạn 2");
    }

    // ===================== test data =====================

    // Report bắt buộc có title: Map.of không nhận giá trị null
    private ReportGenerationInput reportInput() {
        ReportGenerationInput input = new ReportGenerationInput();
        input.setTitle("Báo cáo quý 3");
        return input;
    }
}
