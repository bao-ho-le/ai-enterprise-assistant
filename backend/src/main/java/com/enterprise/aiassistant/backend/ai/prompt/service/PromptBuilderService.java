package com.enterprise.aiassistant.backend.ai.prompt.service;

import com.enterprise.aiassistant.backend.ai.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.generation.dto.FormGenerationInput;
import com.enterprise.aiassistant.backend.ai.generation.dto.ReportGenerationInput;
import com.enterprise.aiassistant.backend.ai.generation.dto.SummaryGenerationInput;
import org.springframework.stereotype.Service;

import java.util.List;

// Builds the raw prompt text fed to LLMService.generate(...). One place for prompt
// shape so swapping FakeLLMService for a real model doesn't touch the handlers/QA flow.
@Service
public class PromptBuilderService {

    // Shared "plain text only, no Markdown/HTML/XML/rich text" core that every
    // generation type's Base Instructions build on top of — one place for the
    // constraint instead of copy-pasting it per generation type. The frontend only
    // ever renders raw text (e.g. GeneratedContentPreview.js: `whitespace-pre-wrap`,
    // no Markdown renderer), so unrendered Markdown syntax would otherwise leak
    // through as literal "#"/"**"/etc. characters.
    private static final String PLAIN_TEXT_INSTRUCTIONS = """
            Output plain text only. Do not use Markdown, HTML, XML, or any other rich-text \
            formatting anywhere in the response: no "#", "##", "###", "**", "_", Markdown \
            bullet markers ("-", "*"), Markdown numbered lists, block quotes (">"), code \
            blocks, or any other Markdown/formatting characters.
            
            Do not add unnecessary whitespace or blank lines. Break lines sensibly between \
            ideas/paragraphs so the text reads well as plain text.""";

    // Standing instructions appended to every email regardless of length/audience/tone.
    private static final String EMAIL_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            This is a professional email. Use a normal plain-text email format — greeting, \
            body, closing, and signature — written as plain sentences and paragraphs, never \
            as Markdown headings or lists.""";

    // Standing instructions appended to every report regardless of length/audience/tone.
    // Forces plain-text output (no Markdown) with numbered headings so every report
    // renders consistently on the frontend.
    private static final String REPORT_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            This is a professional business report. Number every section heading as a \
            plain line, for example "1. Executive Summary" then "2. Background", \
            continuing sequentially through the last section. A heading is only the \
            number, a period, a space, and the title — never prefix it with "#", "##", \
            "###", or any other symbol, and never bold or underline it.
            
            Leave exactly one blank line between the report title, each heading, and each \
            section's content — no extra blank lines, no trailing whitespace. Keep the \
            information coherent, do not repeat the same point in multiple sections, and \
            always end with a numbered "Conclusion" section.""";

    // Standing instructions appended to every summary regardless of length/audience/style.
    private static final String SUMMARY_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            This is a document summary. If presented as bullet points, use a plain-text \
            bullet marker ("•") or an ordinary number followed by a period — never a \
            Markdown "-" or "*" marker. If presented under headings, write each heading as \
            a plain line of text — never prefixed with "#", "##", "###", or any other \
            symbol.""";

    // Standing instructions appended to every Document QA answer.
    private static final String DOCUMENT_QA_BASE_INSTRUCTIONS = PLAIN_TEXT_INSTRUCTIONS + """
            
            
            Do not use headings, code blocks, or tables of any kind, Markdown or \
            otherwise. If a list is needed, use plain numbering ("1.", "2.") or a plain \
            bullet ("•") — never a Markdown list marker.
            
            Answer concisely and clearly, based strictly on the provided document \
            excerpts. If the excerpts do not contain enough information to answer, say so \
            plainly instead of guessing or inventing information.
            
            Answer in the same language the question was asked in. If that language is \
            Vietnamese, write entirely in standard Vietnamese with full diacritics (tone \
            marks and vowel marks) on every word — never unaccented Vietnamese ("khong \
            dau") — and do not mix in English words or phrases unless there is no natural \
            Vietnamese equivalent.""";

    public String buildEmailPrompt(EmailGenerationInput input) {
        String tone = valueOrDefault(input.getTone(), "Professional");
        String length = valueOrDefault(input.getLength(), "Medium");
        String audience = valueOrDefault(input.getAudience(), "General Audience");
        String language = valueOrDefault(input.getLanguage(), "English");

        return """
                Draft a professional email.
                Recipient: %s
                Purpose: %s
                Additional context: %s
                Sender: %s
                
                Language: %s. %s
                Tone: %s. %s
                Length: %s. %s
                Audience: %s. %s
                
                %s
                """.formatted(
                valueOrDefault(input.getRecipient(), "the recipient"),
                input.getPurpose(),
                valueOrDefault(input.getOptionalContext(), "None"),
                valueOrDefault(input.getSender(), "[Your Name]"),
                language, languageInstruction(language),
                tone, toneInstruction(tone),
                length, emailLengthInstruction(length),
                audience, audienceInstruction(audience),
                EMAIL_BASE_INSTRUCTIONS
        );
    }

    public String buildReportPrompt(ReportGenerationInput input, String documentContext) {
        String length = valueOrDefault(input.getLength(), "Medium");
        String audience = valueOrDefault(input.getAudience(), "General Audience");
        String language = valueOrDefault(input.getLanguage(), "English");

        return """
                Write a business report titled "%s".
                Instructions: %s
                Reporting period: %s
                
                Language: %s. %s
                Length: %s. %s
                Audience: %s. %s
                
                %s
                
                Source documents:
                %s
                """.formatted(
                input.getTitle(),
                valueOrDefault(input.getInstructions(), "None"),
                dateRange(input.getFromDate(), input.getToDate()),
                language, languageInstruction(language),
                length, reportLengthInstruction(length),
                audience, audienceInstruction(audience),
                REPORT_BASE_INSTRUCTIONS,
                valueOrDefault(documentContext, "No source documents attached.")
        );
    }

    public String buildSummaryPrompt(SummaryGenerationInput input, String documentContext) {
        String length = valueOrDefault(input.getLength(), "Medium");
        String style = valueOrDefault(input.getStyle(), "PARAGRAPH");
        String language = valueOrDefault(input.getLanguage(), "English");

        return """
                Summarize the attached documents.
                Instructions: %s
                Audience: %s
                Include a dedicated action items section: %s
                
                Language: %s. %s
                Length: %s. %s
                Presentation style: %s. %s
                
                %s
                
                Source documents:
                %s
                """.formatted(
                valueOrDefault(input.getInstructions(), "None"),
                valueOrDefault(input.getAudience(), "General audience"),
                Boolean.TRUE.equals(input.getIncludeActionItems()) ? "Yes" : "No",
                language, languageInstruction(language),
                length, summaryLengthInstruction(length),
                style, summaryStyleInstruction(style),
                SUMMARY_BASE_INSTRUCTIONS,
                valueOrDefault(documentContext, "No source documents attached.")
        );
    }

    public String buildFormPrompt(FormGenerationInput input) {
        return """
                Design a form for the following purpose: %s
                Desired fields: %s
                """.formatted(
                input.getPurpose(),
                valueOrDefault(input.getFields(), "Infer reasonable fields from the purpose.")
        );
    }

    public String buildDocumentQaPrompt(String question, List<String> contextChunks) {

        String context = contextChunks == null || contextChunks.isEmpty()
                ? "No relevant passages were found."
                : String.join("\n---\n", contextChunks);

        return """
                Answer the following question using only the provided document excerpts. Question: %s
                
                %s
                
                Document excerpts:
                %s
                """.formatted(question, DOCUMENT_QA_BASE_INSTRUCTIONS, context);
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
