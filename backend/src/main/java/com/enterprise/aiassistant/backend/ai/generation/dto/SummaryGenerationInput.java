package com.enterprise.aiassistant.backend.ai.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SummaryGenerationInput {

    // EXECUTIVE | BULLET_POINTS | TIMELINE | ACTION_ITEMS — free text, no enum
    // constraint so the fake model can be pointed at anything without a migration.
    @NotBlank
    private String style;

    @Size(max = 2000)
    private String instructions;

    // Short | Medium | Long
    private String length;

    @Size(max = 200)
    private String audience;

    private String language;

    private Boolean includeActionItems;
}
