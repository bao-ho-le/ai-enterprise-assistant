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

    public String buildEmailPrompt(EmailGenerationInput input) {
        return """
                Draft a professional email.
                Recipient: %s
                Purpose: %s
                Additional context: %s
                Tone: %s
                Length: %s
                Language: %s
                Audience: %s
                Sender: %s
                """.formatted(
                valueOrDefault(input.getRecipient(), "the recipient"),
                input.getPurpose(),
                valueOrDefault(input.getOptionalContext(), "None"),
                valueOrDefault(input.getTone(), "Formal"),
                valueOrDefault(input.getLength(), "Medium"),
                valueOrDefault(input.getLanguage(), "English"),
                valueOrDefault(input.getAudience(), "General"),
                valueOrDefault(input.getSender(), "[Your Name]")
        );
    }

    public String buildReportPrompt(ReportGenerationInput input, String documentContext) {
        return """
                Write a business report titled "%s".
                Instructions: %s
                Audience: %s
                Tone: %s
                Length: %s
                Reporting period: %s
                Language: %s

                Source documents:
                %s
                """.formatted(
                input.getTitle(),
                valueOrDefault(input.getInstructions(), "None"),
                valueOrDefault(input.getAudience(), "General audience"),
                valueOrDefault(input.getTone(), "Formal"),
                valueOrDefault(input.getLength(), "Medium"),
                dateRange(input.getFromDate(), input.getToDate()),
                valueOrDefault(input.getLanguage(), "English"),
                valueOrDefault(documentContext, "No source documents attached.")
        );
    }

    public String buildSummaryPrompt(SummaryGenerationInput input, String documentContext) {
        return """
                Summarize the attached documents in "%s" style.
                Instructions: %s
                Audience: %s
                Length: %s
                Language: %s
                Include a dedicated action items section: %s

                Source documents:
                %s
                """.formatted(
                input.getStyle(),
                valueOrDefault(input.getInstructions(), "None"),
                valueOrDefault(input.getAudience(), "General audience"),
                valueOrDefault(input.getLength(), "Medium"),
                valueOrDefault(input.getLanguage(), "English"),
                Boolean.TRUE.equals(input.getIncludeActionItems()) ? "Yes" : "No",
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

                Document excerpts:
                %s
                """.formatted(question, context);
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
}
