package com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.FormGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.ReportGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Chữ của mọi prompt nằm trong resources/prompts/*.txt (chỗ trống dạng {{biến}}), class này
// chỉ tính giá trị để điền vào. Prompt nào cũng có khối "chỉ xuất text thuần, không Markdown"
// vì frontend hiển thị text thô (vd GeneratedContentPreview.js: `whitespace-pre-wrap`).
//
// Prompt chat (general chat, QA, intent...) do ChatAssistant tự đọc file. 4 prompt generation
// dưới đây được điền sẵn bằng PromptTemplate rồi gửi thẳng qua ChatModel: tài liệu đính kèm có
// thể chứa chuỗi "{{...}}", mà đưa text đã điền vào AI Service thì LangChain4j coi nó là template
// lần nữa và báo lỗi "Value for the variable ... is missing".
@Service
public class PromptBuilderService {

    private final PromptTemplate emailTemplate = load("prompts/email.txt");
    private final PromptTemplate reportTemplate = load("prompts/report.txt");
    private final PromptTemplate summaryTemplate = load("prompts/summary.txt");
    private final PromptTemplate formTemplate = load("prompts/form.txt");

    public String buildEmailPrompt(EmailGenerationInput input) {
        String tone = valueOrDefault(input.getTone(), "Professional");
        String length = valueOrDefault(input.getLength(), "Medium");
        String audience = valueOrDefault(input.getAudience(), "General Audience");
        String language = valueOrDefault(input.getLanguage(), "English");

        // 12 biến, quá giới hạn 10 cặp của Map.of
        Map<String, Object> variables = new HashMap<>();
        variables.put("recipient", valueOrDefault(input.getRecipient(), "the recipient"));
        variables.put("purpose", input.getPurpose());
        variables.put("optionalContext", valueOrDefault(input.getOptionalContext(), "None"));
        variables.put("sender", valueOrDefault(input.getSender(), "[Your Name]"));
        variables.put("language", language);
        variables.put("languageInstruction", languageInstruction(language));
        variables.put("tone", tone);
        variables.put("toneInstruction", toneInstruction(tone));
        variables.put("length", length);
        variables.put("lengthInstruction", emailLengthInstruction(length));
        variables.put("audience", audience);
        variables.put("audienceInstruction", audienceInstruction(audience));

        return emailTemplate.apply(variables).text();
    }

    public String buildReportPrompt(ReportGenerationInput input, String documentContext) {
        String length = valueOrDefault(input.getLength(), "Medium");
        String audience = valueOrDefault(input.getAudience(), "General Audience");
        String language = valueOrDefault(input.getLanguage(), "English");

        return reportTemplate.apply(Map.of(
                "title", input.getTitle(),
                "instructions", valueOrDefault(input.getInstructions(), "None"),
                "period", dateRange(input.getFromDate(), input.getToDate()),
                "language", language,
                "languageInstruction", languageInstruction(language),
                "length", length,
                "lengthInstruction", reportLengthInstruction(length),
                "audience", audience,
                "audienceInstruction", audienceInstruction(audience),
                "documentContext", valueOrDefault(documentContext, "No source documents attached.")
        )).text();
    }

    public String buildSummaryPrompt(SummaryGenerationInput input, String documentContext) {
        String length = valueOrDefault(input.getLength(), "Medium");
        String style = valueOrDefault(input.getStyle(), "PARAGRAPH");
        String language = valueOrDefault(input.getLanguage(), "English");

        return summaryTemplate.apply(Map.of(
                "instructions", valueOrDefault(input.getInstructions(), "None"),
                "audience", valueOrDefault(input.getAudience(), "General audience"),
                "includeActionItems", Boolean.TRUE.equals(input.getIncludeActionItems()) ? "Yes" : "No",
                "language", language,
                "languageInstruction", languageInstruction(language),
                "length", length,
                "lengthInstruction", summaryLengthInstruction(length),
                "style", style,
                "styleInstruction", summaryStyleInstruction(style),
                "documentContext", valueOrDefault(documentContext, "No source documents attached.")
        )).text();
    }

    public String buildFormPrompt(FormGenerationInput input) {
        return formTemplate.apply(Map.of(
                "purpose", input.getPurpose(),
                "fields", valueOrDefault(input.getFields(), "Infer reasonable fields from the purpose.")
        )).text();
    }

    // Phần "Document excerpts" trong prompt Document QA (prompts/document-qa.txt).
    public String buildDocumentExcerpts(List<String> contextChunks) {
        return contextChunks == null || contextChunks.isEmpty()
                ? "No relevant passages were found."
                : String.join("\n---\n", contextChunks);
    }

    // Đọc UTF-8 tường minh: prompt có tiếng Việt và các ký tự "—", "•".
    public static PromptTemplate load(String path) {
        try {
            return PromptTemplate.from(new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String dateRange(String fromDate, String toDate) {
        if ((fromDate == null || fromDate.isBlank()) && (toDate == null || toDate.isBlank())) {
            return "Not specified";
        }
        return valueOrDefault(fromDate, "…") + " to " + valueOrDefault(toDate, "…");
    }

    // Shared by email/report/summary prompts. Models sometimes drop Vietnamese tone marks
    // ("Kinh gui" instead of "Kính gửi") on longer generations, so Vietnamese gets an
    // explicit, unambiguous nudge instead of relying on the bare language name.
    private String languageInstruction(String language) {
        return switch (language.trim()) {
            case "Vietnamese" -> "Write entirely in standard Vietnamese with full diacritics " +
                    "(tone marks and vowel marks) on every word — never write unaccented " +
                    "Vietnamese (\"khong dau\"). Do not mix in English words or phrases unless " +
                    "there is no natural Vietnamese equivalent. Use natural, native-sounding " +
                    "Vietnamese phrasing throughout, including in headings and closings.";
            default -> "Write entirely in " + language + ".";
        };
    }

    // Email-only — Write Report has no Tone field.
    private String toneInstruction(String tone) {
        return switch (tone.trim()) {
            case "Formal" -> "Use formal, dignified language.";
            case "Friendly" -> "Sound warm and friendly while staying professional.";
            case "Persuasive" -> "Focus on persuading the reader through strong arguments and clear benefits.";
            case "Apologetic" -> "Show genuine empathy, a sincere apology, and a clear path to resolution.";
            default -> "Sound objective, clear, and trustworthy (professional tone).";
        };
    }

    // Shared by email and report prompts — audience options overlap; unrecognized/blank
    // values (including plain "General Audience") fall back to the general-audience case.
    private String audienceInstruction(String audience) {
        return switch (audience.trim()) {
            case "Internal Team" ->
                    "Write for internal colleagues: you may use internal terminology and assume shared context.";
            case "Business Partner" -> "Write for a business partner: emphasize collaboration and shared goals.";
            case "Customer" ->
                    "Write for a customer: be polite, explain things clearly, and highlight benefits and next steps.";
            case "Executive", "Executive Leadership" ->
                    "Write for executives: focus on decisions, business impact, risks, and recommendations.";
            case "Board of Directors" ->
                    "Write for the board of directors: focus on governance-level decisions, business impact, risks, and recommendations.";
            default -> "Write for a general audience: use plain, accessible language and avoid jargon.";
        };
    }

    private String emailLengthInstruction(String length) {
        return switch (length.trim()) {
            case "Short" -> "About 5-8 sentences. Focus only on the essential information for a concise email.";
            case "Long" ->
                    "About 18-25 sentences across 4-6 paragraphs, with full explanations, supporting information, and next steps where relevant.";
            default ->
                    "About 10-15 sentences across 2-3 paragraphs, with enough context, explanation, and a clear closing.";
        };
    }

    private String reportLengthInstruction(String length) {
        return switch (length.trim()) {
            case "Short" -> """
                    Keep the report concise with exactly these 3 numbered sections in order:
                    1. Executive Summary (1 short paragraph)
                    2. Key Findings (2-3 short paragraphs)
                    3. Conclusion (1 short paragraph)
                    
                    Target approximately 300-500 words in total.
                    Focus only on the most important information and avoid unnecessary detail.
                    """;

            case "Long" -> """
                    Produce a comprehensive report with exactly these 8 numbered sections in order:
                    1. Executive Summary (1-2 paragraphs)
                    2. Background (2-3 paragraphs)
                    3. Objectives (1-2 paragraphs)
                    4. Analysis (4-6 paragraphs)
                    5. Key Findings (3-4 paragraphs)
                    6. Risks & Challenges (2-3 paragraphs)
                    7. Recommendations (2-3 paragraphs)
                    8. Conclusion (1-2 paragraphs)
                    
                    Target approximately 1200-1800 words in total.
                    Each section should provide sufficient detail while remaining focused and avoiding repetition.
                    """;

            default -> """
                    Produce a well-balanced report with exactly these 5 numbered sections in order:
                    1. Executive Summary (1 paragraph)
                    2. Background (2 paragraphs)
                    3. Analysis (3-4 paragraphs)
                    4. Key Findings (2-3 paragraphs)
                    5. Recommendations (2 paragraphs)
                    
                    Target approximately 700-1000 words in total.
                    Maintain a good balance between readability and detail.
                    """;
        };
    }

    private String summaryLengthInstruction(String length) {
        return switch (length.trim()) {
            case "Short" -> "About 100-150 words, covering only the most important points.";
            case "Long" -> "About 400-600 words, covering all the important content in full.";
            default -> "About 200-350 words, covering the main content.";
        };
    }

    private String summaryStyleInstruction(String style) {
        return switch (style.trim().toUpperCase()) {
            case "BULLET_POINTS" ->
                    "Present the summary as bullet points; each bullet should cover exactly one main idea.";
            case "STRUCTURED" -> "Present the summary under numbered headings, for example " +
                    "\"1. Overview\" then \"2. Key Findings\", \"3. Important Details\", \"4. Conclusion\", " +
                    "continuing sequentially (5., 6., ...) if there are more headings. Each heading is " +
                    "only the number, a period, a space, and the title — plain text, never \"#\", \"##\", " +
                    "\"###\", or any other Markdown symbol.";
            default -> "Present the summary as natural, well-connected paragraphs.";
        };
    }
}
