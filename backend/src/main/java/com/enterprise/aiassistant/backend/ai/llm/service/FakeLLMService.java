package com.enterprise.aiassistant.backend.ai.llm.service;

import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.dto.TokenUsage;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import org.springframework.stereotype.Service;

// ponytail: template-per-type stand-in for a real LLM call. Swap the body of generate()
// for a real provider call later; callers only ever depend on the LLMService interface.
@Service
public class FakeLLMService implements LLMService {

    private static final String MODEL_NAME = "fake-llm-v1";
    private static final int EXCERPT_LENGTH = 220;

    @Override
    public LLMResponse generate(LLMRequest request) {

        String content = buildContent(request.getConversationType(), request.getPrompt());

        return LLMResponse.builder()
                .content(content)
                .modelName(MODEL_NAME)
                .tokenUsage(estimateUsage(request.getPrompt(), content))
                .build();
    }

    private String buildContent(ConversationType type, String prompt) {

        String excerpt = excerpt(prompt);

        if (type == null) {
            return excerpt;
        }

        return switch (type) {
            case EMAIL_GENERATION -> """
                    Dear Team,

                    I hope this message finds you well. Following up on your request:

                    %s

                    Please let me know if you have any questions or need further details.

                    Best regards,
                    [Your Name]"""
                    .formatted(excerpt);

            case REPORT_GENERATION -> """
                    # Report

                    ## Overview
                    %s

                    ## Key Findings
                    - Finding 1: derived from the attached source documents.
                    - Finding 2: consistent with the stated focus of this report.
                    - Finding 3: no material risks identified in the reviewed material.

                    ## Conclusion
                    Based on the information reviewed, the report supports the objectives outlined above."""
                    .formatted(excerpt);

            case SUMMARY_GENERATION -> """
                    Summary

                    - %s
                    - The source material covers the main points relevant to the requested scope.
                    - No conflicting information was found across the reviewed documents.

                    Overall, the content aligns with the stated summary objective."""
                    .formatted(excerpt);

            case FORM_GENERATION -> """
                    Form: Generated Fields

                    1. Full Name (text, required)
                    2. Contact Email (text, required)
                    3. %s
                    4. Additional Comments (textarea, optional)

                    Submit button: "Submit\""""
                    .formatted(excerpt);

            case DOCUMENT_QA -> """
                    Based on the attached documents:

                    %s

                    This answer is grounded in the retrieved source passages above."""
                    .formatted(excerpt);

            default -> excerpt;
        };
    }

    private String excerpt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "No additional details were provided.";
        }
        String trimmed = prompt.trim();
        return trimmed.length() > EXCERPT_LENGTH ? trimmed.substring(0, EXCERPT_LENGTH) + "..." : trimmed;
    }

    private TokenUsage estimateUsage(String prompt, String content) {
        int input = Math.max(1, (prompt == null ? 0 : prompt.length()) / 4);
        int output = Math.max(1, content.length() / 4);
        return TokenUsage.builder().inputTokens(input).outputTokens(output).build();
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
